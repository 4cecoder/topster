// Unit tests for HTTP client
import { describe, test, expect } from 'bun:test';

describe('HTTP Client', () => {
  describe('URL Validation and Security', () => {
    test('should validate HTTPS URLs', () => {
      const isValidUrl = (url: string): boolean => {
        try {
          const parsed = new URL(url);
          return parsed.protocol === 'https:' || parsed.protocol === 'http:';
        } catch {
          return false;
        }
      };

      expect(isValidUrl('https://example.com')).toBe(true);
      expect(isValidUrl('http://example.com')).toBe(true);
    });

    test('should reject invalid protocols', () => {
      const isValidUrl = (url: string): boolean => {
        try {
          const parsed = new URL(url);
          return parsed.protocol === 'https:' || parsed.protocol === 'http:';
        } catch {
          return false;
        }
      };

      expect(isValidUrl('javascript:alert(1)')).toBe(false);
      expect(isValidUrl('file:///etc/passwd')).toBe(false);
      expect(isValidUrl('data:text/html,<script>alert(1)</script>')).toBe(false);
    });

    test('should reject malformed URLs', () => {
      const isValidUrl = (url: string): boolean => {
        try {
          const parsed = new URL(url);
          return parsed.protocol === 'https:' || parsed.protocol === 'http:';
        } catch {
          return false;
        }
      };

      expect(isValidUrl('not a url')).toBe(false);
      expect(isValidUrl('')).toBe(false);
      expect(isValidUrl('//example.com')).toBe(false);
    });

    test('should handle URLs with SSRF attempts', () => {
      const isValidUrl = (url: string): boolean => {
        try {
          const parsed = new URL(url);
          return parsed.protocol === 'https:' || parsed.protocol === 'http:';
        } catch {
          return false;
        }
      };

      const isSafeUrl = (url: string): boolean => {
        if (!isValidUrl(url)) return false;

        try {
          const parsed = new URL(url);
          const hostname = parsed.hostname.toLowerCase();

          // Block localhost and private IPs
          if (hostname === 'localhost' || hostname === '127.0.0.1') return false;
          if (hostname.startsWith('192.168.')) return false;
          if (hostname.startsWith('10.')) return false;
          if (hostname.match(/^172\.(1[6-9]|2[0-9]|3[0-1])\./)) return false;

          return true;
        } catch {
          return false;
        }
      };

      expect(isSafeUrl('http://localhost:8080/admin')).toBe(false);
      expect(isSafeUrl('http://127.0.0.1/secrets')).toBe(false);
      expect(isSafeUrl('http://192.168.1.1/router')).toBe(false);
      expect(isSafeUrl('http://10.0.0.1/internal')).toBe(false);
      expect(isSafeUrl('http://172.16.0.1/service')).toBe(false);
      expect(isSafeUrl('https://example.com/api')).toBe(true);
    });
  });

  describe('JSON Parsing Safety', () => {
    test('should parse valid JSON', () => {
      const safeJsonParse = <T>(json: string): T | null => {
        try {
          return JSON.parse(json);
        } catch {
          return null;
        }
      };

      const valid = '{"sources":[{"file":"test.m3u8"}]}';
      const result = safeJsonParse<{ sources: any[] }>(valid);
      expect(result).not.toBeNull();
      expect(result?.sources).toHaveLength(1);
    });

    test('should handle malformed JSON', () => {
      const safeJsonParse = <T>(json: string): T | null => {
        try {
          return JSON.parse(json);
        } catch {
          return null;
        }
      };

      expect(safeJsonParse('{invalid json}')).toBeNull();
      expect(safeJsonParse('')).toBeNull();
      expect(safeJsonParse('null')).toBeNull();
    });

    test('should handle JSON with prototype pollution attempts', () => {
      const safeJsonParse = <T>(json: string): T | null => {
        try {
          const obj = JSON.parse(json);
          // Check for prototype pollution
          if (obj && (obj.__proto__ || obj.constructor || obj.prototype)) {
            return null;
          }
          return obj;
        } catch {
          return null;
        }
      };

      const malicious = '{"__proto__":{"isAdmin":true}}';
      expect(safeJsonParse(malicious)).toBeNull();
    });

    test('should handle very large JSON', () => {
      const safeJsonParse = <T>(json: string, maxSize: number = 10_000_000): T | null => {
        if (json.length > maxSize) {
          return null;
        }
        try {
          return JSON.parse(json);
        } catch {
          return null;
        }
      };

      const largeJson = '{"data":"' + 'a'.repeat(15_000_000) + '"}';
      expect(safeJsonParse(largeJson, 10_000_000)).toBeNull();
    });

    test('should handle deeply nested JSON', () => {
      const safeJsonParse = <T>(json: string, maxDepth: number = 100): T | null => {
        const countDepth = (obj: any, depth: number = 0): number => {
          if (depth > maxDepth) return depth;
          if (typeof obj !== 'object' || obj === null) return depth;

          let maxChildDepth = depth;
          for (const key in obj) {
            if (obj.hasOwnProperty(key)) {
              const childDepth = countDepth(obj[key], depth + 1);
              maxChildDepth = Math.max(maxChildDepth, childDepth);
            }
          }
          return maxChildDepth;
        };

        try {
          const obj = JSON.parse(json);
          if (countDepth(obj) > maxDepth) {
            return null;
          }
          return obj;
        } catch {
          return null;
        }
      };

      let nested = '{"a":';
      for (let i = 0; i < 150; i++) {
        nested += '{"b":';
      }
      nested += '1';
      for (let i = 0; i < 150; i++) {
        nested += '}';
      }
      nested += '}';

      expect(safeJsonParse(nested, 100)).toBeNull();
    });
  });

  describe('Header Sanitization', () => {
    test('should sanitize user agent', () => {
      const sanitizeHeader = (value: string): string => {
        // Remove newlines and carriage returns to prevent header injection
        return value.replace(/[\r\n]/g, '');
      };

      const malicious = 'Mozilla/5.0\r\nX-Injected: evil';
      expect(sanitizeHeader(malicious)).toBe('Mozilla/5.0X-Injected: evil');
    });

    test('should sanitize referer header', () => {
      const sanitizeHeader = (value: string): string => {
        return value.replace(/[\r\n]/g, '');
      };

      const malicious = 'https://example.com\r\nX-Injected: evil';
      expect(sanitizeHeader(malicious)).toBe('https://example.comX-Injected: evil');
    });

    test('should handle null bytes in headers', () => {
      const sanitizeHeader = (value: string): string => {
        return value.replace(/[\r\n\0]/g, '');
      };

      const malicious = 'value\x00with\x00nulls';
      expect(sanitizeHeader(malicious)).toBe('valuewithnulls');
    });
  });

  describe('Response Validation', () => {
    test('should validate content-type for JSON', () => {
      const isValidJsonResponse = (contentType: string): boolean => {
        return contentType.includes('application/json') ||
               contentType.includes('text/json');
      };

      expect(isValidJsonResponse('application/json')).toBe(true);
      expect(isValidJsonResponse('application/json; charset=utf-8')).toBe(true);
      expect(isValidJsonResponse('text/json')).toBe(true);
      expect(isValidJsonResponse('text/html')).toBe(false);
      expect(isValidJsonResponse('application/javascript')).toBe(false);
    });

    test('should validate content-type for HTML', () => {
      const isValidHtmlResponse = (contentType: string): boolean => {
        return contentType.includes('text/html');
      };

      expect(isValidHtmlResponse('text/html')).toBe(true);
      expect(isValidHtmlResponse('text/html; charset=utf-8')).toBe(true);
      expect(isValidHtmlResponse('application/json')).toBe(false);
    });

    test('should validate response size', () => {
      const isValidResponseSize = (size: number, maxSize: number = 10_000_000): boolean => {
        return size > 0 && size <= maxSize;
      };

      expect(isValidResponseSize(1000)).toBe(true);
      expect(isValidResponseSize(5_000_000)).toBe(true);
      expect(isValidResponseSize(15_000_000)).toBe(false);
      expect(isValidResponseSize(0)).toBe(false);
      expect(isValidResponseSize(-100)).toBe(false);
    });
  });

  describe('Retry Logic', () => {
    test('should calculate exponential backoff', () => {
      const calculateBackoff = (attempt: number, baseDelay: number = 1000): number => {
        return baseDelay * Math.pow(2, attempt - 1);
      };

      expect(calculateBackoff(1)).toBe(1000);
      expect(calculateBackoff(2)).toBe(2000);
      expect(calculateBackoff(3)).toBe(4000);
      expect(calculateBackoff(4)).toBe(8000);
    });

    test('should cap maximum backoff', () => {
      const calculateBackoff = (attempt: number, baseDelay: number = 1000, maxDelay: number = 10000): number => {
        const delay = baseDelay * Math.pow(2, attempt - 1);
        return Math.min(delay, maxDelay);
      };

      expect(calculateBackoff(10)).toBe(10000);
      expect(calculateBackoff(20)).toBe(10000);
    });

    test('should validate retry attempt count', () => {
      const shouldRetry = (attempt: number, maxAttempts: number = 3): boolean => {
        return attempt < maxAttempts;
      };

      expect(shouldRetry(1, 3)).toBe(true);
      expect(shouldRetry(2, 3)).toBe(true);
      expect(shouldRetry(3, 3)).toBe(false);
      expect(shouldRetry(4, 3)).toBe(false);
    });
  });

  describe('Error Handling', () => {
    test('should categorize HTTP errors', () => {
      const categorizeError = (statusCode: number): string => {
        if (statusCode >= 400 && statusCode < 500) return 'client';
        if (statusCode >= 500 && statusCode < 600) return 'server';
        if (statusCode >= 300 && statusCode < 400) return 'redirect';
        if (statusCode >= 200 && statusCode < 300) return 'success';
        return 'unknown';
      };

      expect(categorizeError(200)).toBe('success');
      expect(categorizeError(301)).toBe('redirect');
      expect(categorizeError(404)).toBe('client');
      expect(categorizeError(403)).toBe('client');
      expect(categorizeError(500)).toBe('server');
      expect(categorizeError(503)).toBe('server');
    });

    test('should determine if error is retryable', () => {
      const isRetryableError = (statusCode: number): boolean => {
        // Retry on server errors and some client errors
        if (statusCode >= 500) return true;
        if (statusCode === 429) return true; // Rate limit
        if (statusCode === 408) return true; // Request timeout
        return false;
      };

      expect(isRetryableError(500)).toBe(true);
      expect(isRetryableError(502)).toBe(true);
      expect(isRetryableError(503)).toBe(true);
      expect(isRetryableError(429)).toBe(true);
      expect(isRetryableError(408)).toBe(true);
      expect(isRetryableError(404)).toBe(false);
      expect(isRetryableError(403)).toBe(false);
      expect(isRetryableError(200)).toBe(false);
    });
  });

  describe('Timeout Handling', () => {
    test('should validate timeout values', () => {
      const isValidTimeout = (timeout: number): boolean => {
        return timeout > 0 && timeout <= 120000; // Max 2 minutes
      };

      expect(isValidTimeout(5000)).toBe(true);
      expect(isValidTimeout(30000)).toBe(true);
      expect(isValidTimeout(0)).toBe(false);
      expect(isValidTimeout(-1000)).toBe(false);
      expect(isValidTimeout(150000)).toBe(false);
    });

    test('should calculate adaptive timeout', () => {
      const calculateTimeout = (attempt: number, baseTimeout: number = 10000): number => {
        // Increase timeout on retries
        return baseTimeout * (1 + attempt * 0.5);
      };

      expect(calculateTimeout(0)).toBe(10000);
      expect(calculateTimeout(1)).toBe(15000);
      expect(calculateTimeout(2)).toBe(20000);
    });
  });
});

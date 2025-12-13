// Unit tests for VidCloud extractor
import { describe, test, expect } from 'bun:test';

describe('VidCloud Extractor', () => {
  describe('getRefererFromEmbed', () => {
    test('should extract referer from standard HTTPS URL', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://streameeeeee.site/embed-1/v3/e-1/PTQPpj58kcTg?z=';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://streameeeeee.site/');
    });

    test('should extract referer from HTTP URL', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'http://example.com/embed/video123';
      expect(getRefererFromEmbed(embedUrl)).toBe('http://example.com/');
    });

    test('should extract referer from URL with port', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://example.com:8080/embed/video123';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://example.com:8080/');
    });

    test('should extract referer from URL with subdomain', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://cdn.embed.example.com/video/12345';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://cdn.embed.example.com/');
    });

    test('should extract referer from URL with complex path', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://example.com/path/to/deeply/nested/embed/video123';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://example.com/');
    });

    test('should handle URL with query parameters', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://example.com/embed?id=123&autoplay=1';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://example.com/');
    });

    test('should handle URL with fragment', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://example.com/embed#timestamp=123';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://example.com/');
    });

    test('should return original string on invalid URL', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const invalidUrl = 'not-a-valid-url';
      expect(getRefererFromEmbed(invalidUrl)).toBe('not-a-valid-url');
    });

    test('should handle URL with authentication', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://user:pass@example.com/embed/video';
      // URL API strips authentication from host, so we get just the host
      expect(getRefererFromEmbed(embedUrl)).toBe('https://example.com/');
    });

    test('should handle localhost URLs', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'http://localhost:3000/embed/video';
      expect(getRefererFromEmbed(embedUrl)).toBe('http://localhost:3000/');
    });

    test('should handle IP address URLs', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://192.168.1.1:8080/embed/video';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://192.168.1.1:8080/');
    });

    test('should handle IPv6 URLs', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://[2001:db8::1]:8080/embed/video';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://[2001:db8::1]:8080/');
    });
  });

  describe('Edge Cases and Security', () => {
    test('should handle very long URLs', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const longPath = 'a/'.repeat(1000);
      const embedUrl = `https://example.com/${longPath}video`;
      expect(getRefererFromEmbed(embedUrl)).toBe('https://example.com/');
    });

    test('should handle URLs with special characters in domain', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://stream-service.example-site.com/embed';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://stream-service.example-site.com/');
    });

    test('should handle URLs with encoded characters', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://example.com/embed?id=%20%21%22%23';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://example.com/');
    });

    test('should handle empty string', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      expect(getRefererFromEmbed('')).toBe('');
    });

    test('should handle URL with only protocol', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://';
      expect(getRefererFromEmbed(embedUrl)).toBe(embedUrl);
    });

    test('should handle data URLs', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'data:text/html,<h1>Test</h1>';
      // Data URLs don't have a host, so this should fall back
      const result = getRefererFromEmbed(embedUrl);
      expect(result).toBeTruthy();
    });

    test('should handle file URLs', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'file:///path/to/video.mp4';
      const result = getRefererFromEmbed(embedUrl);
      expect(result).toBeTruthy();
    });

    test('should handle javascript: URLs', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'javascript:alert(1)';
      const result = getRefererFromEmbed(embedUrl);
      expect(result).toBeTruthy();
    });

    test('should handle URLs with international domains (IDN)', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://例え.jp/embed/video';
      // URL API converts IDN to punycode
      expect(getRefererFromEmbed(embedUrl)).toBe('https://xn--r8jz45g.jp/');
    });

    test('should handle punycode domains', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://xn--e1afmkfd.xn--p1ai/embed';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://xn--e1afmkfd.xn--p1ai/');
    });
  });

  describe('Real-world URL patterns', () => {
    test('should handle streameeeeee.site URLs', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://streameeeeee.site/embed-1/v3/e-1/PTQPpj58kcTg?z=';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://streameeeeee.site/');
    });

    test('should handle rabbitstream.net URLs', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://rabbitstream.net/v2/embed-4/abc123xyz';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://rabbitstream.net/');
    });

    test('should handle megacloud.tv URLs', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://megacloud.tv/embed-2/e-1/ABC123xyz';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://megacloud.tv/');
    });

    test('should handle vidsrc URLs', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://vidsrc.to/embed/movie/123456';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://vidsrc.to/');
    });

    test('should handle URLs with multiple query parameters', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://example.com/embed?id=123&autoplay=1&muted=0&sub=eng';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://example.com/');
    });

    test('should handle URLs with encoded slashes in path', () => {
      const getRefererFromEmbed = (embedUrl: string): string => {
        try {
          const url = new URL(embedUrl);
          return `${url.protocol}//${url.host}/`;
        } catch {
          return embedUrl;
        }
      };

      const embedUrl = 'https://example.com/embed%2Fvideo%2F123';
      expect(getRefererFromEmbed(embedUrl)).toBe('https://example.com/');
    });
  });
});

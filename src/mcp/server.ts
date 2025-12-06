// SSE MCP Server using Elysia.js

import { Elysia } from 'elysia';
import { cors } from '@elysiajs/cors';
import { tools, executeTool } from './tools';
import { getConfig } from '../core/config';

interface JsonRpcRequest {
  jsonrpc: '2.0';
  id: string | number;
  method: string;
  params?: Record<string, unknown>;
}

interface JsonRpcResponse {
  jsonrpc: '2.0';
  id: string | number;
  result?: unknown;
  error?: {
    code: number;
    message: string;
    data?: unknown;
  };
}

interface SSEClient {
  controller: ReadableStreamDefaultController<Uint8Array>;
  encoder: TextEncoder;
}

const SERVER_INFO = {
  name: 'topster',
  version: '1.0.0',
  protocolVersion: '2025-06-18',
};

const CAPABILITIES = {
  tools: {},
};

// Store active SSE connections by session ID
const sseClients = new Map<string, SSEClient>();

function sendToClient(sessionId: string, data: JsonRpcResponse) {
  const client = sseClients.get(sessionId);
  if (client) {
    try {
      const event = `event: message\ndata: ${JSON.stringify(data)}\n\n`;
      client.controller.enqueue(client.encoder.encode(event));
      console.log(`[SSE] Sent to ${sessionId}:`, JSON.stringify(data));
    } catch (err) {
      console.error(`[SSE] Failed to send to ${sessionId}:`, err);
    }
  } else {
    console.warn(`[SSE] No client found for session: ${sessionId}`);
  }
}

export function createMcpServer(port: number = 3847) {
  const app = new Elysia()
    .use(cors())
    .get('/health', () => ({ status: 'ok', server: SERVER_INFO }))

    // SSE endpoint for MCP
    .get('/sse', ({ set, request }) => {
      const clientIp = request.headers.get('x-forwarded-for') || 'unknown';
      const userAgent = request.headers.get('user-agent') || 'unknown';
      const sessionId = crypto.randomUUID();

      console.log(`[SSE] Client connected: ${sessionId}`);
      console.log(`[SSE]   IP: ${clientIp}`);
      console.log(`[SSE]   User-Agent: ${userAgent}`);

      set.headers['content-type'] = 'text/event-stream';
      set.headers['cache-control'] = 'no-cache';
      set.headers['connection'] = 'keep-alive';

      const encoder = new TextEncoder();
      let pingInterval: ReturnType<typeof setInterval> | null = null;

      const stream = new ReadableStream({
        start(controller) {
          // Store this client
          sseClients.set(sessionId, { controller, encoder });

          console.log(`[SSE] Stream started for: ${sessionId}`);
          // Send endpoint event with session ID in query param
          const event = `event: endpoint\ndata: /messages?sessionId=${sessionId}\n\n`;
          controller.enqueue(encoder.encode(event));

          // Keep connection alive with periodic pings
          pingInterval = setInterval(() => {
            try {
              controller.enqueue(encoder.encode(': ping\n\n'));
            } catch {
              if (pingInterval) clearInterval(pingInterval);
            }
          }, 15000);
        },
        cancel() {
          console.log(`[SSE] Client disconnected: ${sessionId}`);
          sseClients.delete(sessionId);
          if (pingInterval) {
            clearInterval(pingInterval);
            pingInterval = null;
          }
        },
      });

      return new Response(stream, {
        headers: {
          'Content-Type': 'text/event-stream',
          'Cache-Control': 'no-cache',
          'Connection': 'keep-alive',
        },
      });
    })

    // Message endpoint for JSON-RPC
    .post('/messages', async ({ body, query }) => {
      const request = body as JsonRpcRequest;
      const sessionId = query.sessionId as string;

      console.log(`[MCP] Request (session: ${sessionId}): ${request.method}`, request.params ? JSON.stringify(request.params) : '');

      let responseData: JsonRpcResponse;
      try {
        responseData = await handleRequest(request);
      } catch (err) {
        responseData = {
          jsonrpc: '2.0',
          id: request.id,
          error: {
            code: -32603,
            message: err instanceof Error ? err.message : 'Internal error',
          },
        };
      }

      console.log(`[MCP] Response:`, JSON.stringify(responseData));

      // Send response via SSE stream if we have a session
      if (sessionId && sseClients.has(sessionId)) {
        sendToClient(sessionId, responseData);
        // Return 202 Accepted to indicate async response via SSE
        return new Response(null, { status: 202 });
      }

      // Fallback: return response directly (for testing without SSE)
      return responseData;
    })

    // Alternative REST-style endpoint for simpler integration
    .post('/rpc', async ({ body }) => {
      const request = body as JsonRpcRequest;
      return await handleRequest(request);
    });

  return app;
}

async function handleRequest(request: JsonRpcRequest): Promise<JsonRpcResponse> {
  const { method, params, id } = request;

  switch (method) {
    case 'initialize':
      return {
        jsonrpc: '2.0',
        id,
        result: {
          ...SERVER_INFO,
          capabilities: CAPABILITIES,
        },
      };

    case 'initialized':
      return {
        jsonrpc: '2.0',
        id,
        result: {},
      };

    case 'tools/list':
      return {
        jsonrpc: '2.0',
        id,
        result: { tools },
      };

    case 'tools/call': {
      const toolName = params?.name as string;
      const toolArgs = (params?.arguments as Record<string, unknown>) || {};

      if (!toolName) {
        return {
          jsonrpc: '2.0',
          id,
          error: {
            code: -32602,
            message: 'Missing tool name',
          },
        };
      }

      const result = await executeTool(toolName, toolArgs);
      return {
        jsonrpc: '2.0',
        id,
        result,
      };
    }

    case 'ping':
      return {
        jsonrpc: '2.0',
        id,
        result: {},
      };

    default:
      return {
        jsonrpc: '2.0',
        id,
        error: {
          code: -32601,
          message: `Method not found: ${method}`,
        },
      };
  }
}

export async function startMcpServer(port?: number): Promise<void> {
  const config = getConfig();
  const serverPort = port || config.get('mcpPort');
  const host = config.get('mcpHost');

  const app = createMcpServer(serverPort);

  app.listen(serverPort, () => {
    console.log(`Topster MCP Server running at http://${host}:${serverPort}`);
    console.log(`SSE endpoint: http://${host}:${serverPort}/sse`);
    console.log(`Messages endpoint: http://${host}:${serverPort}/messages`);
    console.log(`Health check: http://${host}:${serverPort}/health`);
    console.log('\nPress Ctrl+C to stop');
  });

  // Keep process running
  await new Promise(() => {});
}

#!/usr/bin/env python3
import http.server
import socketserver
import os
from urllib.parse import unquote

class UploadHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        file_data = self.rfile.read(content_length)

        # Get filename from URL path or use default
        filename = unquote(self.path.lstrip('/'))
        if not filename or filename == '/':
            filename = 'uploaded_file'

        # Write file
        with open(filename, 'wb') as f:
            f.write(file_data)

        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        self.wfile.write(f'File {filename} uploaded successfully ({len(file_data)} bytes)'.encode())

PORT = 8001
with socketserver.TCPServer(("", PORT), UploadHTTPRequestHandler) as httpd:
    print(f"Server running on port {PORT}")
    httpd.serve_forever()

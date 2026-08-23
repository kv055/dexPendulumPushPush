#!/usr/bin/env python3
"""Mock pushpush server for the compatibility test.

Logs every POST as a JSON line: {"path", "secret_ok", "body"}.
Usage: mock_pushpush.py --port PORT --log LOGFILE [--secret EXPECTED]
"""
import argparse
import http.server
import json
import threading


class Handler(http.server.BaseHTTPRequestHandler):
    expected_secret = "unset"

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length).decode("utf-8", "replace")
        entry = {
            "path": self.path,
            "secret_ok": self.headers.get("X-Webhook-Secret") == self.expected_secret,
            "body": body,
        }
        with open(self.server.log_path, "a") as f:
            f.write(json.dumps(entry) + "\n")
            f.flush()
        self.send_response(200)
        self.end_headers()

    def do_GET(self):
        self.send_response(200)
        self.end_headers()

    def log_message(self, *args):
        pass


class MockServer(http.server.HTTPServer):
    daemon_threads = True
    log_path = "/dev/null"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, required=True)
    parser.add_argument("--log", required=True)
    parser.add_argument("--secret", default="test-secret-123")
    args = parser.parse_args()

    Handler.expected_secret = args.secret
    MockServer.log_path = args.log
    server = MockServer(("127.0.0.1", args.port), Handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    print(f"mock pushpush listening on 127.0.0.1:{args.port}", flush=True)
    try:
        while True:
            threading.Event().wait(3600)
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()

from __future__ import annotations

import argparse
import json
import os
import socket
import sys
import time
from pathlib import Path

from . import __version__
from .api_client import AgentApiClient
from .hardware_probe import probe


def main() -> None:
    parser = argparse.ArgumentParser(prog="edge-agent")
    parser.add_argument("--control-plane-url", default=os.environ.get("EDGE_AGENT_CONTROL_PLANE_URL"))
    parser.add_argument("--credential-file", default="/etc/edge-agent/device-credential")
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("doctor")
    enroll = commands.add_parser("enroll"); enroll.add_argument("--token-stdin", action="store_true"); enroll.add_argument("--display-name", default=socket.gethostname())
    run = commands.add_parser("run"); run.add_argument("--heartbeat-interval-seconds", type=int, default=30)
    commands.add_parser("version")
    args = parser.parse_args()
    if args.command == "version": print(__version__); return
    facts = probe()
    if args.command == "doctor": print(json.dumps(facts, indent=2, sort_keys=True)); return
    if not args.control_plane_url: parser.error("--control-plane-url or EDGE_AGENT_CONTROL_PLANE_URL is required")
    if args.command == "run":
        credential_file = Path(args.credential_file)
        try:
            credential = credential_file.read_text(encoding="utf-8").strip()
        except OSError as error:
            parser.error(f"cannot read device credential: {error}")
        if not credential:
            parser.error("device credential file is empty")
        client = AgentApiClient(args.control_plane_url, credential)
        while True:
            # V1 foundation only: claiming and executing jobs are added after lease APIs exist.
            client.heartbeat(facts)
            time.sleep(args.heartbeat_interval_seconds)
    if not args.token_stdin: parser.error("enroll requires --token-stdin")
    token = sys.stdin.readline().strip()
    if not token:
        parser.error("no enrollment token was supplied on standard input")
    result = AgentApiClient(args.control_plane_url).enroll(token, args.display_name, facts)
    credential_file = Path(args.credential_file); credential_file.parent.mkdir(parents=True, exist_ok=True); credential_file.write_text(result["credential"], encoding="utf-8"); credential_file.chmod(0o600)
    print(f"Enrolled {result['deviceId']} with status {result['status']}.")


if __name__ == "__main__": main()

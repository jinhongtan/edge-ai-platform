from __future__ import annotations

import json
import time
import urllib.error
import urllib.request
from typing import Any


class AgentApiClient:
    def __init__(self, base_url: str, credential: str | None = None) -> None:
        self._base_url = base_url.rstrip("/")
        self._credential = credential

    def enroll(self, token: str, display_name: str, capabilities: dict[str, Any]) -> dict[str, Any]:
        return self._request("POST", "/api/agent/v1/enroll", {"token": token, "displayName": display_name, "capabilities": capabilities}, authenticated=False)

    def heartbeat(self, capabilities: dict[str, Any]) -> dict[str, Any]:
        return self._request("POST", "/api/agent/v1/heartbeat", {"capabilities": capabilities})

    def _request(self, method: str, path: str, body: dict[str, Any], authenticated: bool = True) -> dict[str, Any]:
        for attempt in range(3):
            headers = {"Content-Type": "application/json"}
            if authenticated and self._credential:
                headers["Authorization"] = f"Bearer {self._credential}"
            request = urllib.request.Request(self._base_url + path, data=json.dumps(body).encode(), headers=headers, method=method)
            try:
                with urllib.request.urlopen(request, timeout=20) as response:
                    return json.loads(response.read())["data"]
            except urllib.error.HTTPError as error:
                if error.code in (401, 403) or error.code < 500 or attempt == 2:
                    raise RuntimeError(f"Control plane rejected request ({error.code})") from error
            except urllib.error.URLError as error:
                if attempt == 2:
                    raise RuntimeError("Control plane is unavailable") from error
            time.sleep(0.25 * (2 ** attempt))
        raise AssertionError("unreachable")

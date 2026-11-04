from pathlib import Path

import pytest

from edge_agent.config import AgentConfig
from edge_agent.hardware_probe import _read_text


def test_config_requires_https() -> None:
    config = AgentConfig("http://localhost:8080", Path("credential"), Path("work"))
    with pytest.raises(ValueError, match="HTTPS"):
        config.validate()


def test_probe_model_reader_handles_missing_device_tree() -> None:
    assert _read_text(Path("this-device-tree-file-does-not-exist")) is None

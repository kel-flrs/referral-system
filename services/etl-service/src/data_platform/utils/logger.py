"""Structured logging utilities for data pipeline."""

import logging
import structlog
from structlog.processors import JSONRenderer, TimeStamper, add_log_level


def configure_logger():
    """Configure structured logging."""
    structlog.configure(
        processors=[
            TimeStamper(fmt="iso"),
            add_log_level,
            structlog.processors.StackInfoRenderer(),
            structlog.processors.format_exc_info,
            JSONRenderer()
        ],
        wrapper_class=structlog.make_filtering_bound_logger(logging.INFO),
        context_class=dict,
        logger_factory=structlog.PrintLoggerFactory(),
        cache_logger_on_first_use=True,
    )


def get_logger(name: str):
    """Get a structured logger instance."""
    return structlog.get_logger(name)

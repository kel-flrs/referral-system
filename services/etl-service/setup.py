"""Setup configuration for ETL Service."""

from setuptools import setup, find_packages
import os

# Read requirements if exists
requirements = []
if os.path.exists("src/data_platform/requirements.txt"):
    with open("src/data_platform/requirements.txt") as f:
        requirements = f.read().splitlines()

setup(
    name="etl-service",
    version="0.1.0",
    description="ETL service for syncing data from various CRM systems",
    author="Referral System Team",
    packages=find_packages(where="src"),
    package_dir={"": "src"},
    python_requires=">=3.11",
    install_requires=requirements,
    extras_require={
        "dev": [
            "pytest>=7.4.0",
            "pytest-cov>=4.1.0",
            "black>=23.7.0",
            "flake8>=6.1.0",
            "mypy>=1.5.0",
        ],
    },
)

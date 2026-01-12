"""Setup configuration for ML Service."""

from setuptools import setup, find_packages

with open("requirements.txt") as f:
    requirements = f.read().splitlines()

setup(
    name="ml-service",
    version="0.1.0",
    description="ML-based candidate matching service using semantic embeddings",
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
    entry_points={
        "console_scripts": [
            "ml-service=main:main",
        ],
    },
)

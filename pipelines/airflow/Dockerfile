FROM apache/airflow:2.8.1-python3.11

USER root

# Install system dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and data platform modules
COPY data_platform/requirements.txt /opt/airflow/requirements.txt
COPY data_platform /opt/airflow/data_platform
COPY airflow/scripts /opt/airflow/scripts

# Fix permissions
RUN chown -R airflow:root /opt/airflow/requirements.txt && \
    chown -R airflow:root /opt/airflow/data_platform && \
    chown -R airflow:root /opt/airflow/scripts && \
    chmod +x /opt/airflow/scripts/init-airflow.sh

USER airflow

# Install Python dependencies
RUN pip install --no-cache-dir -r /opt/airflow/requirements.txt

WORKDIR /opt/airflow

"""Extracts data from Bullhorn Mock Service API."""

import os
from typing import Dict, List, Optional
from datetime import datetime

import requests
from tenacity import retry, stop_after_attempt, wait_exponential

from data_platform.utils.logger import get_logger

logger = get_logger(__name__)


class BullhornExtractor:
    """Handles data extraction from Bullhorn Mock Service."""

    def __init__(self):
        self.base_url = os.getenv('BULLHORN_MOCK_BASE_URL', 'http://localhost:8080')
        self.client_id = os.getenv('BULLHORN_MOCK_CLIENT_ID', 'your-client-id')
        self.client_secret = os.getenv('BULLHORN_MOCK_CLIENT_SECRET', 'your-client-secret')
        self.username = os.getenv('BULLHORN_MOCK_USERNAME', 'admin')
        self.password = os.getenv('BULLHORN_MOCK_PASSWORD', 'admin')
        self.bh_rest_token: Optional[str] = None
        self.token_expiry: Optional[datetime] = None

        logger.info("BullhornExtractor initialized", base_url=self.base_url)

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def authenticate(self) -> str:
        """
        Authenticate with Bullhorn Mock Service using OAuth 2.0 flow.

        Returns:
            BhRestToken for authenticated API calls
        """
        logger.info("Starting OAuth authentication flow")

        # Step 1: Get authorization code
        auth_response = requests.get(
            f"{self.base_url}/oauth/authorize",
            params={
                'client_id': self.client_id,
                'username': self.username,
                'password': self.password,
                'response_type': 'code',
                'action': 'Login'
            },
            timeout=30
        )
        auth_response.raise_for_status()
        auth_code = auth_response.json()['code']
        logger.info("Authorization code obtained")

        # Step 2: Exchange code for access token
        token_response = requests.post(
            f"{self.base_url}/oauth/token",
            data={
                'grant_type': 'authorization_code',
                'code': auth_code,
                'client_id': self.client_id,
                'client_secret': self.client_secret
            },
            timeout=30
        )
        token_response.raise_for_status()
        access_token = token_response.json()['access_token']
        logger.info("Access token obtained")

        # Step 3: Get BhRestToken
        login_response = requests.post(
            f"{self.base_url}/rest-services/login",
            params={
                'version': '*',
                'access_token': access_token
            },
            timeout=30
        )
        login_response.raise_for_status()

        login_data = login_response.json()
        self.bh_rest_token = login_data.get('BhRestToken') or login_data.get('bhRestToken')

        logger.info("BhRestToken obtained successfully")
        return self.bh_rest_token

    def _make_authenticated_request(
        self,
        endpoint: str,
        params: Optional[Dict] = None,
        method: str = 'GET',
        json_data: Optional[Dict] = None
    ) -> Dict:
        """
        Make authenticated API request with BhRestToken in header.

        Args:
            endpoint: API endpoint path
            params: Query parameters
            method: HTTP method (GET, POST, PUT, DELETE)
            json_data: JSON request body for POST/PUT requests
        """
        if not self.bh_rest_token:
            self.authenticate()

        headers = {
            'BhRestToken': self.bh_rest_token,
            'Content-Type': 'application/json'
        }

        # Make request with specified method
        response = requests.request(
            method=method,
            url=f"{self.base_url}{endpoint}",
            headers=headers,
            params=params,
            json=json_data,
            timeout=120
        )

        # If unauthorized, re-authenticate and retry once
        if response.status_code == 401:
            logger.warning("Token expired, re-authenticating")
            self.authenticate()
            headers['BhRestToken'] = self.bh_rest_token
            response = requests.request(
                method=method,
                url=f"{self.base_url}{endpoint}",
                headers=headers,
                params=params,
                json=json_data,
                timeout=120
            )

        response.raise_for_status()
        return response.json()

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def extract_consultants(self, page: int = 0, size: int = 500000) -> List[Dict]:
        """
        Extract consultant data from Bullhorn.

        Args:
            page: Page number for pagination
            size: Page size

        Returns:
            List of consultant records
        """
        logger.info("Extracting consultants", page=page, size=size)

        response = self._make_authenticated_request(
            '/api/v1/consultants',
            params={'page': page, 'size': size}
        )

        # Handle nested response structure: { data: { content: [...] } }
        consultants = []
        if isinstance(response, dict):
            if 'data' in response and isinstance(response['data'], dict):
                if 'content' in response['data']:
                    consultants = response['data']['content']
            elif 'content' in response:
                consultants = response['content']
        elif isinstance(response, list):
            consultants = response

        logger.info("Consultants extracted", count=len(consultants))
        return consultants

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def extract_candidates(self, page: int = 0, size: int = 500000) -> List[Dict]:
        """
        Extract candidate data from Bullhorn.

        Args:
            page: Page number for pagination
            size: Page size

        Returns:
            List of candidate records
        """
        logger.info("Extracting candidates", page=page, size=size)

        response = self._make_authenticated_request(
            '/api/v1/candidates',
            params={'page': page, 'size': size}
        )

        # Handle nested response structure
        candidates = []
        if isinstance(response, dict):
            if 'data' in response and isinstance(response['data'], dict):
                if 'content' in response['data']:
                    candidates = response['data']['content']
            elif 'content' in response:
                candidates = response['content']
        elif isinstance(response, list):
            candidates = response

        logger.info("Candidates extracted", count=len(candidates))
        return candidates

    def extract_all_data(self) -> Dict[str, List[Dict]]:
        """
        Extract all data from Bullhorn (consultants and candidates).

        Returns:
            Dictionary with 'consultants' and 'candidates' keys
        """
        logger.info("Starting full data extraction")

        data = {
            'consultants': self.extract_consultants(),
            'candidates': self.extract_candidates()
        }

        logger.info(
            "Full data extraction complete",
            consultants_count=len(data['consultants']),
            candidates_count=len(data['candidates'])
        )

        return data

    # ========== OPTIMIZED PAGINATION METHODS (PRODUCTION-GRADE) ==========

    def _extract_content(self, response: Dict) -> List[Dict]:
        """
        Extract content array from various response formats.

        Handles:
        - { data: { content: [...] } }
        - { content: [...] }
        - [...]
        """
        if isinstance(response, dict):
            if 'data' in response and isinstance(response['data'], dict):
                if 'content' in response['data']:
                    return response['data']['content']
            elif 'content' in response:
                return response['content']
        elif isinstance(response, list):
            return response

        return []

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def extract_consultants_paginated(
        self,
        page_size: int = 1000,
        max_records: Optional[int] = None,
        start_page: int = 0
    ) -> List[Dict]:
        """
        Extract consultant data from Bullhorn with pagination (OPTIMIZED).

        Production-optimized with:
        - Pagination to avoid timeouts
        - Progress tracking
        - Memory efficiency
        - Fault tolerance

        Args:
            page_size: Records per page (default 1000, max recommended)
            max_records: Maximum records to fetch (None = all)
            start_page: Starting page number (for resuming)

        Returns:
            List of consultant records
        """
        import time

        logger.info(
            "Starting paginated consultant extraction",
            page_size=page_size,
            max_records=max_records,
            start_page=start_page
        )

        all_consultants = []
        page = start_page
        total_fetched = 0
        consecutive_empty_pages = 0

        while True:
            try:
                logger.info(
                    "Fetching consultant page",
                    page=page,
                    total_fetched=total_fetched,
                    target=max_records or "all"
                )

                response = self._make_authenticated_request(
                    '/api/v1/consultants',
                    params={'page': page, 'size': page_size}
                )

                # Extract consultants from response
                consultants = self._extract_content(response)

                if not consultants:
                    consecutive_empty_pages += 1
                    logger.warning(
                        "Empty page received",
                        page=page,
                        consecutive_empty=consecutive_empty_pages
                    )

                    # Stop if we get 3 consecutive empty pages
                    if consecutive_empty_pages >= 3:
                        logger.info("Multiple empty pages, stopping extraction")
                        break

                    page += 1
                    continue

                # Reset empty page counter
                consecutive_empty_pages = 0

                all_consultants.extend(consultants)
                total_fetched += len(consultants)

                logger.info(
                    "Page fetched successfully",
                    page=page,
                    records_in_page=len(consultants),
                    total_fetched=total_fetched
                )

                # Check if we've hit max_records limit
                if max_records and total_fetched >= max_records:
                    logger.info(
                        "Max records reached, stopping",
                        total_fetched=total_fetched
                    )
                    break

                # Check if this was the last page (partial page)
                if len(consultants) < page_size:
                    logger.info(
                        "Last page reached (partial page)",
                        records_in_page=len(consultants),
                        total_fetched=total_fetched
                    )
                    break

                page += 1

                # Rate limiting - be nice to the API
                time.sleep(0.1)  # 100ms between requests

            except Exception as e:
                logger.error(
                    "Error fetching page",
                    page=page,
                    error=str(e),
                    total_fetched_before_error=total_fetched
                )
                # Re-raise to trigger retry logic
                raise

        logger.info(
            "Consultant extraction complete",
            total_records=len(all_consultants),
            total_pages=page - start_page + 1
        )

        return all_consultants

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
    def extract_candidates_paginated(
        self,
        page_size: int = 1000,
        max_records: Optional[int] = None,
        start_page: int = 0
    ) -> List[Dict]:
        """
        Extract candidate data from Bullhorn with pagination (OPTIMIZED).

        Production-optimized with:
        - Pagination to avoid timeouts
        - Progress tracking
        - Memory efficiency
        - Fault tolerance

        Args:
            page_size: Records per page (default 1000, max recommended)
            max_records: Maximum records to fetch (None = all)
            start_page: Starting page number (for resuming)

        Returns:
            List of candidate records
        """
        import time

        logger.info(
            "Starting paginated candidate extraction",
            page_size=page_size,
            max_records=max_records,
            start_page=start_page
        )

        all_candidates = []
        page = start_page
        total_fetched = 0
        consecutive_empty_pages = 0

        while True:
            try:
                logger.info(
                    "Fetching candidate page",
                    page=page,
                    total_fetched=total_fetched,
                    target=max_records or "all"
                )

                response = self._make_authenticated_request(
                    '/api/v1/candidates',
                    params={'page': page, 'size': page_size}
                )

                # Extract candidates from response
                candidates = self._extract_content(response)

                if not candidates:
                    consecutive_empty_pages += 1
                    logger.warning(
                        "Empty page received",
                        page=page,
                        consecutive_empty=consecutive_empty_pages
                    )

                    if consecutive_empty_pages >= 3:
                        logger.info("Multiple empty pages, stopping extraction")
                        break

                    page += 1
                    continue

                consecutive_empty_pages = 0
                all_candidates.extend(candidates)
                total_fetched += len(candidates)

                logger.info(
                    "Page fetched successfully",
                    page=page,
                    records_in_page=len(candidates),
                    total_fetched=total_fetched
                )

                if max_records and total_fetched >= max_records:
                    logger.info(
                        "Max records reached, stopping",
                        total_fetched=total_fetched
                    )
                    break

                if len(candidates) < page_size:
                    logger.info(
                        "Last page reached (partial page)",
                        records_in_page=len(candidates),
                        total_fetched=total_fetched
                    )
                    break

                page += 1

                # Rate limiting
                time.sleep(0.1)

            except Exception as e:
                logger.error(
                    "Error fetching page",
                    page=page,
                    error=str(e),
                    total_fetched_before_error=total_fetched
                )
                raise

        logger.info(
            "Candidate extraction complete",
            total_records=len(all_candidates),
            total_pages=page - start_page + 1
        )

        return all_candidates

    def extract_all_data_paginated(
        self,
        page_size: int = 1000,
        max_records_per_type: Optional[int] = None
    ) -> Dict[str, List[Dict]]:
        """
        Extract all data from Bullhorn with pagination (OPTIMIZED).

        Args:
            page_size: Records per page
            max_records_per_type: Max records per entity type (None = all)

        Returns:
            Dictionary with 'consultants' and 'candidates' keys
        """
        logger.info(
            "Starting full paginated data extraction",
            page_size=page_size,
            max_per_type=max_records_per_type or "unlimited"
        )

        data = {
            'consultants': self.extract_consultants_paginated(
                page_size=page_size,
                max_records=max_records_per_type
            ),
            'candidates': self.extract_candidates_paginated(
                page_size=page_size,
                max_records=max_records_per_type
            )
        }

        logger.info(
            "Full data extraction complete",
            consultants_count=len(data['consultants']),
            candidates_count=len(data['candidates']),
            total_records=len(data['consultants']) + len(data['candidates'])
        )

        return data

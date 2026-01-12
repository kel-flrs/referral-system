/**
 * Custom Error Classes for Better Error Handling
 */

export class AppError extends Error {
  constructor(
    public message: string,
    public statusCode: number = 500,
    public isOperational: boolean = true
  ) {
    super(message);
    this.name = this.constructor.name;
    Error.captureStackTrace(this, this.constructor);
  }
}

export class ValidationError extends AppError {
  constructor(message: string) {
    super(message, 400, true);
  }
}

export class NotFoundError extends AppError {
  constructor(message: string = 'Resource not found') {
    super(message, 404, true);
  }
}

export class UnauthorizedError extends AppError {
  constructor(message: string = 'Unauthorized') {
    super(message, 401, true);
  }
}

export class ForbiddenError extends AppError {
  constructor(message: string = 'Forbidden') {
    super(message, 403, true);
  }
}

export class ConflictError extends AppError {
  constructor(message: string) {
    super(message, 409, true);
  }
}

export class BadRequestError extends AppError {
  constructor(message: string) {
    super(message, 400, true);
  }
}

export class InternalServerError extends AppError {
  constructor(message: string = 'Internal server error') {
    super(message, 500, false);
  }
}

export class ExternalServiceError extends AppError {
  constructor(message: string, public service: string) {
    super(message, 502, true);
  }
}

export class DatabaseError extends AppError {
  constructor(message: string) {
    super(message, 500, false);
  }
}

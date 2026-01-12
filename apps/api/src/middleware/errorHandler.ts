import { Request, Response, NextFunction } from 'express';
import { logger } from '../config/logger';
import { AppError } from '../utils/errors';
import { Prisma } from '@prisma/client';

export function errorHandler(
  err: Error,
  req: Request,
  res: Response,
  next: NextFunction
) {
  // Log error with correlation ID if available
  const correlationId = (req as any).correlationId || 'unknown';

  logger.error('Error occurred:', {
    correlationId,
    message: err.message,
    stack: process.env.NODE_ENV === 'development' ? err.stack : undefined,
    path: req.path,
    method: req.method,
    name: err.name,
  });

  // Handle custom AppError
  if (err instanceof AppError) {
    return res.status(err.statusCode).json({
      error: err.message,
      correlationId,
    });
  }

  // Handle Prisma errors
  if (err instanceof Prisma.PrismaClientKnownRequestError) {
    // P2002: Unique constraint violation
    if (err.code === 'P2002') {
      return res.status(409).json({
        error: 'Resource already exists',
        correlationId,
      });
    }
    // P2025: Record not found
    if (err.code === 'P2025') {
      return res.status(404).json({
        error: 'Resource not found',
        correlationId,
      });
    }
    // P2003: Foreign key constraint failed
    if (err.code === 'P2003') {
      return res.status(400).json({
        error: 'Invalid reference to related resource',
        correlationId,
      });
    }
  }

  if (err instanceof Prisma.PrismaClientValidationError) {
    return res.status(400).json({
      error: 'Invalid data provided',
      correlationId,
    });
  }

  // Handle validation errors (from Zod)
  if (err.name === 'ZodError') {
    return res.status(400).json({
      error: 'Validation failed',
      details: err.message,
      correlationId,
    });
  }

  // Default to 500 server error
  res.status(500).json({
    error: 'Internal server error',
    message: process.env.NODE_ENV === 'development' ? err.message : 'An unexpected error occurred',
    correlationId,
  });
}

export function notFoundHandler(req: Request, res: Response) {
  const correlationId = (req as any).correlationId || 'unknown';

  res.status(404).json({
    error: 'Not found',
    path: req.path,
    correlationId,
  });
}

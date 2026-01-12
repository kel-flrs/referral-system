import { Request, Response, NextFunction } from 'express';
import { randomUUID } from 'crypto';

/**
 * Adds a correlation ID to each request for tracking across logs
 */
export function correlationId(req: Request, res: Response, next: NextFunction) {
  const id = req.get('x-correlation-id') || req.get('x-request-id') || randomUUID();

  // Store on request object
  (req as any).correlationId = id;

  // Send back in response
  res.setHeader('x-correlation-id', id);

  next();
}

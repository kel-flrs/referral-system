import { Request, Response, NextFunction } from 'express';
import { ZodSchema, ZodError } from 'zod';
import { ValidationError } from '../utils/errors';

/**
 * Validation middleware factory
 * Validates request body, query, or params against a Zod schema
 */
export const validate = (schema: ZodSchema, source: 'body' | 'query' | 'params' = 'body') => {
  return (req: Request, res: Response, next: NextFunction) => {
    try {
      const data = source === 'body' ? req.body : source === 'query' ? req.query : req.params;
      const result = schema.parse(data);

      // Replace the source data with validated/transformed data
      if (source === 'body') {
        req.body = result;
      } else if (source === 'query') {
        req.query = result as any;
      } else {
        req.params = result as any;
      }

      next();
    } catch (error) {
      if (error instanceof ZodError) {
        const errors = error.errors.map(err => ({
          field: err.path.join('.'),
          message: err.message,
        }));

        next(new ValidationError(JSON.stringify(errors)));
      } else {
        next(error);
      }
    }
  };
};

/**
 * Sanitize strings to prevent XSS
 */
export const sanitizeString = (str: string): string => {
  return str
    .replace(/[<>]/g, '') // Remove < and >
    .trim();
};

/**
 * Validate and sanitize optional strings
 */
export const sanitizeOptionalString = (str?: string): string | undefined => {
  if (!str) return undefined;
  return sanitizeString(str);
};

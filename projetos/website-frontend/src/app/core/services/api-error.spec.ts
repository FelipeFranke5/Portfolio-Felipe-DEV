import { HttpErrorResponse } from '@angular/common/http';

import { flattenValidationErrors, isAuthError, resolveApiErrorMessage } from './api-error';

const FALLBACK = 'Mensagem padrão.';

function buildError(status: number, body: unknown, statusText = 'Error'): HttpErrorResponse {
  return new HttpErrorResponse({ status, statusText, error: body });
}

describe('flattenValidationErrors', () => {
  it('should flatten the 422 error list into a map of field to messages', () => {
    const error = buildError(422, {
      message: 'The request has failed due to incorrect information in the payload provided.',
      errors: [
        { name: ['The project name should have between 5 and 100 characters'] },
        { stack: ['The stack is required'] },
      ],
    });

    expect(flattenValidationErrors(error)).toEqual({
      name: ['The project name should have between 5 and 100 characters'],
      stack: ['The stack is required'],
    });
  });

  it('should concatenate the messages when the same field appears more than once', () => {
    const error = buildError(422, {
      errors: [{ name: ['Field is required'] }, { name: ['Cannot exceed 50 characters'] }],
    });

    expect(flattenValidationErrors(error)).toEqual({
      name: ['Field is required', 'Cannot exceed 50 characters'],
    });
  });

  it('should return an empty map when the status is not 422', () => {
    const error = buildError(400, { error: 'Bad request', reason: 'Project already exists' });

    expect(flattenValidationErrors(error)).toEqual({});
  });

  it('should return an empty map when the 422 body has no errors list', () => {
    expect(flattenValidationErrors(buildError(422, { message: 'invalid' }))).toEqual({});
    expect(flattenValidationErrors(buildError(422, null))).toEqual({});
  });
});

describe('resolveApiErrorMessage', () => {
  it('should return the reason field for a business error', () => {
    const error = buildError(400, {
      error: 'The request has failed due to incorrect use of PROJECT endpoint(s).',
      reason: "Project with name 'Site Pessoal' already exists",
    });

    expect(resolveApiErrorMessage(error, FALLBACK)).toBe(
      "Project with name 'Site Pessoal' already exists"
    );
  });

  it('should return the reason of a 500, which carries the internal log id', () => {
    const error = buildError(500, {
      error: 'Internal Server Error',
      reason:
        'An unhandled exception occurred while processing your request. Please try again later or contact the system administrator informing the ID: 7e5cbeb7 to obtain support.',
    });

    expect(resolveApiErrorMessage(error, FALLBACK)).toContain('7e5cbeb7');
  });

  it('should join the validation messages for a 422', () => {
    const error = buildError(422, {
      errors: [{ name: ['Field is required'] }, { level: ['Should be positive'] }],
    });

    expect(resolveApiErrorMessage(error, FALLBACK)).toBe('Field is required Should be positive');
  });

  it('should return a connection message when the request never reached the server', () => {
    const error = buildError(0, null, 'Unknown Error');

    expect(resolveApiErrorMessage(error, FALLBACK)).toContain('falar com o servidor');
  });

  it('should fall back to the provided message when the body has no usable field', () => {
    expect(resolveApiErrorMessage(buildError(404, {}), FALLBACK)).toBe(FALLBACK);
    expect(resolveApiErrorMessage(buildError(503, null), FALLBACK)).toBe(FALLBACK);
  });

  it('should use the error field when there is no reason', () => {
    const error = buildError(404, { error: 'Unable to find the project you attempted to retrieve' });

    expect(resolveApiErrorMessage(error, FALLBACK)).toBe(
      'Unable to find the project you attempted to retrieve'
    );
  });
});

describe('isAuthError', () => {
  it('should flag 401 and 403 as authentication problems', () => {
    expect(isAuthError(buildError(401, null))).toBeTrue();
    expect(isAuthError(buildError(403, null))).toBeTrue();
  });

  it('should not flag other statuses', () => {
    expect(isAuthError(buildError(400, null))).toBeFalse();
    expect(isAuthError(buildError(500, null))).toBeFalse();
  });
});

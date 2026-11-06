import type { Request, Response } from 'express';

/** Minimal Express Response double that records status + JSON body. */
export function fakeRes() {
  const res = {
    statusCode: 200,
    body: undefined as any,
    status(code: number) {
      res.statusCode = code;
      return res;
    },
    json(payload: unknown) {
      res.body = payload;
      return res;
    },
  };
  return res as typeof res & Response;
}

export function fakeReq(init: {
  body?: unknown;
  params?: Record<string, string>;
  query?: Record<string, string>;
  headers?: Record<string, string>;
  user?: { id: string; role: string };
}): Request {
  return {
    body: init.body,
    params: init.params ?? {},
    query: init.query ?? {},
    headers: init.headers ?? {},
    user: init.user,
  } as unknown as Request;
}

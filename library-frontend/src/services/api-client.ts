import { StatusCodes } from "http-status-codes";

// Tudo passa pelo api-gateway, que encaminha cada prefixo para o serviço certo.
export const LIBRARY_API_URL = "/library-api";
export const STUDENTS_API_URL = "/students-api";

type RequestOptions = Omit<RequestInit, "body"> & { body?: unknown };

async function extractErrorMessage(response: Response): Promise<string> {
    const fallback = `Request failed with status ${response.status}`;
    try {
        const contentType = response.headers.get("content-type");
        if (contentType?.includes("application/json")) {
            const data = await response.json();
            return data.message || fallback;
        }
        return (await response.text()) || fallback;
    } catch {
        return fallback;
    }
}

async function request<T>(baseUrl: string, path: string, options: RequestOptions = {}): Promise<T> {
    const response = await fetch(`${baseUrl}${path}`, {
        headers: {
            "Content-Type": "application/json",
            ...(options.headers ?? {}),
        },
        ...options,
        body: options.body ? JSON.stringify(options.body) : undefined,
    });

    if (!response.ok) {
        throw new Error(await extractErrorMessage(response));
    }

    if (response.status === StatusCodes.NO_CONTENT) {
        return undefined as T;
    }

    return (await response.json()) as T;
}

export async function apiClient<T>(path: string, options: RequestOptions = {}): Promise<T> {
    return request<T>(LIBRARY_API_URL, path, options);
}

export async function studentsApiClient<T>(path: string, options: RequestOptions = {}): Promise<T> {
    return request<T>(STUDENTS_API_URL, path, options);
}

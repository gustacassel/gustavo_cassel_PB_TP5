import type { Book, BookInput } from "../types/books";
import { apiClient } from "./api-client";

export async function getBooks(): Promise<Book[]> {
    return apiClient<Book[]>("/api/books")
}

export async function getBookById(id: number): Promise<Book> {
    return apiClient<Book>(`/api/books/${id}`)
}

export async function createBook(payload: BookInput): Promise<Book> {
    return apiClient<Book>("/api/books", {
        method: "POST",
        body: payload,
    })
}

export async function updateBook(id: number, payload: BookInput): Promise<Book> {
    return apiClient<Book>(`/api/books/${id}`, {
        method: "PUT",
        body: payload,
    })
}

export async function deleteBook(id: number): Promise<void> {
    await apiClient<void>(`/api/books/${id}`, {
        method: "DELETE",
    })
}

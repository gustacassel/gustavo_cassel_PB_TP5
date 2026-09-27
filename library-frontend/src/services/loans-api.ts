import type { Loan, LoanInput } from "../types/loans";
import { apiClient } from "./api-client";

export async function getLoans(): Promise<Loan[]> {
    return apiClient<Loan[]>("/api/loans")
}

export async function createLoan(payload: LoanInput): Promise<Loan> {
    return apiClient<Loan>("/api/loans", {
        method: "POST",
        body: payload,
    })
}

export async function returnLoan(id: number): Promise<Loan> {
    return apiClient<Loan>(`/api/loans/${id}/return`, {
        method: "PUT",
    })
}

export async function deleteLoan(id: number): Promise<void> {
    await apiClient<void>(`/api/loans/${id}`, {
        method: "DELETE",
    })
}

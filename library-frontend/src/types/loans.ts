export const LOAN_STATUSES = ["ACTIVE", "RETURNED", "OVERDUE"] as const

export type LoanStatus = (typeof LOAN_STATUSES)[number]

export const LOAN_STATUS_LABELS: Record<LoanStatus, string> = {
    ACTIVE: "Ativo",
    RETURNED: "Devolvido",
    OVERDUE: "Atrasado",
}

export interface Loan {
    id: number
    bookId: number
    bookTitle: string
    bookAuthor: string
    studentId: number
    studentName: string
    studentEnrollmentNumber: string | null
    studentCourseName: string | null
    studentDataAvailable: boolean
    loanDate: string
    dueDate: string
    returnDate: string | null
    status: LoanStatus
}

export interface LoanInput {
    bookId: number
    studentId: number
    loanDate: string | null
    dueDate: string | null
}

import type { Course } from "./courses"

export const STUDENT_STATUSES = ["ATIVO", "TRANCADO", "FORMADO", "DESLIGADO"] as const

export type StudentStatus = (typeof STUDENT_STATUSES)[number]

export const STUDENT_STATUS_LABELS: Record<StudentStatus, string> = {
    ATIVO: "Ativo",
    TRANCADO: "Trancado",
    FORMADO: "Formado",
    DESLIGADO: "Desligado",
}

export const STUDENT_STATUS_VARIANTS: Record<StudentStatus, string> = {
    ATIVO: "success",
    TRANCADO: "warning",
    FORMADO: "info",
    DESLIGADO: "secondary",
}

export interface Student {
    id: number
    name: string
    email: string
    enrollmentNumber: string
    birthDate: string
    enrollmentDate: string
    status: StudentStatus
    currentSemester: number | null
    course: Course | null
    activeLoans: number | null
}

export interface StudentInput {
    name: string
    email: string
    enrollmentNumber: string
    birthDate: string | null
    enrollmentDate?: string | null
    status: StudentStatus
    currentSemester: number | null
    courseId: number | null
}

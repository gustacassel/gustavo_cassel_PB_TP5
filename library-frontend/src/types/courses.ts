export const DEGREE_LEVELS = [
    "TECNICO",
    "GRADUACAO",
    "POS_GRADUACAO",
    "MESTRADO",
    "DOUTORADO",
] as const

export type DegreeLevel = (typeof DEGREE_LEVELS)[number]

export const DEGREE_LEVEL_LABELS: Record<DegreeLevel, string> = {
    TECNICO: "Técnico",
    GRADUACAO: "Graduação",
    POS_GRADUACAO: "Pós-graduação",
    MESTRADO: "Mestrado",
    DOUTORADO: "Doutorado",
}

export interface Course {
    id: number
    name: string
    code: string
    degreeLevel: DegreeLevel
    durationSemesters: number
    department: string | null
}

export interface CourseSummary extends Course {
    activeStudents: number
}

export interface CourseInput {
    name: string
    code: string
    degreeLevel: DegreeLevel
    durationSemesters: number
    department: string | null
}

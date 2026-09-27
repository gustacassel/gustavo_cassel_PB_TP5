import { apiClient } from "./api-client";

// copia local dos alunos na library-api, alimentada pelos eventos de students.events
export interface ReplicatedStudent {
    id: number
    name: string
    email: string | null
    enrollmentNumber: string | null
    status: string
    courseId: number | null
    courseName: string | null
    version: number
    lastEventAt: string
}

export interface StudentReplicaHealth {
    source: string
    studentCount: number
    lastEventAt: string | null
}

export async function getReplicatedStudents(): Promise<ReplicatedStudent[]> {
    return apiClient<ReplicatedStudent[]>("/api/integration/students")
}

export async function getStudentReplicaHealth(): Promise<StudentReplicaHealth> {
    return apiClient<StudentReplicaHealth>("/api/integration/students/health")
}

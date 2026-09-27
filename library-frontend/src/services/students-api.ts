import type { Student, StudentInput } from "../types/students";
import { studentsApiClient } from "./api-client";

export async function getStudents(): Promise<Student[]> {
    return studentsApiClient<Student[]>("/api/students")
}

export async function getStudentById(id: number): Promise<Student> {
    return studentsApiClient<Student>(`/api/students/${id}`)
}

export async function getStudentsByCourse(courseId: number): Promise<Student[]> {
    return studentsApiClient<Student[]>(`/api/students/course/${courseId}`)
}

export async function createStudent(payload: StudentInput): Promise<Student> {
    return studentsApiClient<Student>("/api/students", {
        method: "POST",
        body: payload,
    })
}

export async function updateStudent(id: number, payload: StudentInput): Promise<Student> {
    return studentsApiClient<Student>(`/api/students/${id}`, {
        method: "PUT",
        body: payload,
    })
}

export async function deleteStudent(id: number): Promise<void> {
    await studentsApiClient<void>(`/api/students/${id}`, {
        method: "DELETE",
    })
}

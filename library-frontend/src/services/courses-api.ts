import type { Course, CourseInput, CourseSummary } from "../types/courses";
import { studentsApiClient } from "./api-client";

export async function getCourses(): Promise<Course[]> {
    return studentsApiClient<Course[]>("/api/courses")
}

export async function getCourseSummary(): Promise<CourseSummary[]> {
    return studentsApiClient<CourseSummary[]>("/api/courses/summary")
}

export async function createCourse(payload: CourseInput): Promise<Course> {
    return studentsApiClient<Course>("/api/courses", {
        method: "POST",
        body: payload,
    })
}

export async function updateCourse(id: number, payload: CourseInput): Promise<Course> {
    return studentsApiClient<Course>(`/api/courses/${id}`, {
        method: "PUT",
        body: payload,
    })
}

export async function deleteCourse(id: number): Promise<void> {
    await studentsApiClient<void>(`/api/courses/${id}`, {
        method: "DELETE",
    })
}

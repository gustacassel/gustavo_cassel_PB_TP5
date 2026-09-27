import { useCallback, useEffect, useMemo, useState } from "react"
import { Badge, Button, Spinner, Table } from "react-bootstrap"
import Swal from "sweetalert2"
import PageHeader from "../../components/PageHeader"
import StatCard from "../../components/StatCard"
import { getCourses } from "../../services/courses-api"
import { createStudent, deleteStudent, getStudents, updateStudent } from "../../services/students-api"
import type { Course } from "../../types/courses"
import {
    STUDENT_STATUSES,
    STUDENT_STATUS_LABELS,
    STUDENT_STATUS_VARIANTS,
    type Student,
    type StudentInput,
    type StudentStatus,
} from "../../types/students"
import { errorMessage, escapeHtml, toInputDate } from "../../utils/format"

const courseOptions = (courses: Course[], selectedId: number | null) =>
    ['<option value="">Sem curso</option>']
        .concat(
            courses.map(
                (course) =>
                    `<option value="${course.id}" ${course.id === selectedId ? "selected" : ""}>${escapeHtml(
                        `${course.name} (${course.code})`,
                    )}</option>`,
            ),
        )
        .join("")

const statusOptions = (selected: StudentStatus) =>
    STUDENT_STATUSES.map(
        (status) =>
            `<option value="${status}" ${status === selected ? "selected" : ""}>${
                STUDENT_STATUS_LABELS[status]
            }</option>`,
    ).join("")

const readForm = (): StudentInput | null => {
    const value = (id: string) => (document.getElementById(id) as HTMLInputElement | null)?.value ?? ""

    const name = value("student-name").trim()
    const email = value("student-email").trim()
    const enrollmentNumber = value("student-enrollment").trim()
    const birthDate = value("student-birth")
    const semester = value("student-semester")
    const courseId = value("student-course")
    const status = value("student-status") as StudentStatus

    if (!name || !email || !enrollmentNumber) {
        Swal.showValidationMessage("Nome, email e matrícula são obrigatórios.")
        return null
    }

    return {
        name,
        email,
        enrollmentNumber,
        birthDate: birthDate || null,
        status: status || "ATIVO",
        currentSemester: semester ? Number(semester) : null,
        courseId: courseId ? Number(courseId) : null,
    }
}

const formHtml = (courses: Course[], student?: Student) => `
    <input id="student-name" class="swal2-input" placeholder="Nome" value="${escapeHtml(student?.name ?? "")}">
    <input id="student-email" class="swal2-input" placeholder="Email" value="${escapeHtml(student?.email ?? "")}">
    <input id="student-enrollment" class="swal2-input" placeholder="Matrícula" value="${escapeHtml(
        student?.enrollmentNumber ?? "",
    )}">
    <label class="swal-form-label" for="student-birth">Data de nascimento</label>
    <input id="student-birth" class="swal2-input" type="date" value="${toInputDate(student?.birthDate)}">
    <label class="swal-form-label" for="student-course">Curso</label>
    <select id="student-course" class="swal2-select">${courseOptions(courses, student?.course?.id ?? null)}</select>
    <label class="swal-form-label" for="student-status">Situação</label>
    <select id="student-status" class="swal2-select">${statusOptions(student?.status ?? "ATIVO")}</select>
    <input id="student-semester" class="swal2-input" placeholder="Semestre atual" type="number" min="1" value="${
        student?.currentSemester ?? 1
    }">
`

export default function StudentList() {
    const [students, setStudents] = useState<Student[]>([])
    const [courses, setCourses] = useState<Course[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const fetchData = useCallback(async () => {
        try {
            const [studentData, courseData] = await Promise.all([getStudents(), getCourses()])
            setStudents(studentData)
            setCourses(courseData)
            setError(null)
        } catch (err) {
            setError(
                `${errorMessage(err, "Não foi possível carregar os estudantes.")} - verifique se a students-api está no ar.`,
            )
        } finally {
            setIsLoading(false)
        }
    }, [])

    const load = useCallback(async () => {
        setIsLoading(true)
        await fetchData()
    }, [fetchData])

    useEffect(() => {
        const run = async () => {
            await fetchData()
        }
        run()
    }, [fetchData])

    const activeStudents = useMemo(() => students.filter((student) => student.status === "ATIVO").length, [students])
    const withLoans = useMemo(() => students.filter((student) => (student.activeLoans ?? 0) > 0).length, [students])
    const totalLoans = useMemo(
        () => students.reduce((sum, student) => sum + (student.activeLoans ?? 0), 0),
        [students],
    )

    const handleAddStudent = async () => {
        const result = await Swal.fire({
            title: "Novo estudante",
            html: formHtml(courses),
            focusConfirm: false,
            showCancelButton: true,
            confirmButtonText: "Salvar",
            cancelButtonText: "Cancelar",
            preConfirm: readForm,
        })

        if (!result.isConfirmed || !result.value) {
            return
        }

        try {
            await createStudent(result.value)
            await load()
            await Swal.fire("Salvo", "Estudante cadastrado. A library-api recebe o evento student.created.", "success")
        } catch (err) {
            await Swal.fire("Erro", errorMessage(err, "Não foi possível salvar o estudante."), "error")
        }
    }

    const handleEditStudent = async (student: Student) => {
        const result = await Swal.fire({
            title: "Editar estudante",
            html: formHtml(courses, student),
            focusConfirm: false,
            showCancelButton: true,
            confirmButtonText: "Atualizar",
            cancelButtonText: "Cancelar",
            preConfirm: readForm,
        })

        if (!result.isConfirmed || !result.value) {
            return
        }

        try {
            await updateStudent(student.id, result.value)
            await load()
            await Swal.fire("Atualizado", "Estudante atualizado. A library-api recebe o evento student.updated.", "success")
        } catch (err) {
            await Swal.fire("Erro", errorMessage(err, "Não foi possível atualizar o estudante."), "error")
        }
    }

    const handleDeleteStudent = async (student: Student) => {
        const result = await Swal.fire({
            title: "Remover estudante?",
            text: `Isso vai excluir "${student.name}".`,
            icon: "warning",
            showCancelButton: true,
            confirmButtonText: "Excluir",
            cancelButtonText: "Cancelar",
        })

        if (!result.isConfirmed) {
            return
        }

        try {
            await deleteStudent(student.id)
            await load()
            await Swal.fire("Excluído", "Estudante removido.", "success")
        } catch (err) {
            // 409 quando o aluno ainda tem emprestimo ativo na biblioteca
            await Swal.fire("Não foi possível excluir", errorMessage(err, "Erro ao remover o estudante."), "error")
        }
    }

    return (
        <>
            <PageHeader
                eyebrow="students-api · publica em students.events"
                title="Estudantes"
                description="Dono dos dados de alunos. Cada alteração vira um evento e a contagem de empréstimos chega pelos eventos da biblioteca."
                actions={
                    <Button variant="primary" onClick={handleAddStudent}>
                        <i className="bi bi-person-plus me-2" />
                        Novo estudante
                    </Button>
                }
            />

            <div className="stat-grid">
                <StatCard label="Total de estudantes" value={students.length} icon="bi-people" isLoading={isLoading} />
                <StatCard label="Ativos" value={activeStudents} icon="bi-person-check" tone="success" isLoading={isLoading} />
                <StatCard
                    label="Com livros em mãos"
                    value={withLoans}
                    icon="bi-bookmark-check"
                    tone="accent"
                    hint={`${totalLoans} empréstimo(s) ativo(s)`}
                    isLoading={isLoading}
                />
            </div>

            <section className="data-card">
                <div className="data-card-header">
                    <div>
                        <h2 className="data-card-title">Registro de estudantes</h2>
                        <span className="data-card-subtitle">Servido pela students-api, rota /students-api do gateway</span>
                    </div>
                    {isLoading ? (
                        <Spinner animation="border" size="sm" />
                    ) : (
                        <Button variant="outline-primary" size="sm" onClick={load}>
                            <i className="bi bi-arrow-clockwise me-1" />
                            Atualizar
                        </Button>
                    )}
                </div>

                {error ? (
                    <div className="data-card-body">
                        <div className="alert alert-danger mb-0">{error}</div>
                    </div>
                ) : (
                    <Table responsive className="data-table">
                        <thead>
                            <tr>
                                <th>Estudante</th>
                                <th>Matrícula</th>
                                <th>Curso</th>
                                <th>Situação</th>
                                <th className="text-center">Empréstimos ativos</th>
                                <th className="text-end">Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {students.map((student) => (
                                <tr key={student.id}>
                                    <td>
                                        <span className="cell-main">{student.name}</span>
                                        <span className="cell-sub">{student.email}</span>
                                    </td>
                                    <td>{student.enrollmentNumber}</td>
                                    <td>
                                        {student.course ? (
                                            <>
                                                <span className="cell-main">{student.course.name}</span>
                                                <span className="cell-sub">
                                                    {student.course.code}
                                                    {student.currentSemester ? ` · ${student.currentSemester}º semestre` : ""}
                                                </span>
                                            </>
                                        ) : (
                                            <span className="text-secondary">-</span>
                                        )}
                                    </td>
                                    <td>
                                        <Badge bg={STUDENT_STATUS_VARIANTS[student.status]}>
                                            {STUDENT_STATUS_LABELS[student.status]}
                                        </Badge>
                                    </td>
                                    <td className="text-center">
                                        <span className={`loan-count${(student.activeLoans ?? 0) > 0 ? " has-loans" : ""}`}>
                                            {student.activeLoans ?? 0}
                                        </span>
                                    </td>
                                    <td className="text-end">
                                        <div className="row-actions">
                                            <Button
                                                variant="outline-secondary"
                                                size="sm"
                                                className="icon-btn"
                                                title="Editar"
                                                onClick={() => handleEditStudent(student)}
                                            >
                                                <i className="bi bi-pencil" />
                                            </Button>
                                            <Button
                                                variant="outline-danger"
                                                size="sm"
                                                className="icon-btn"
                                                title="Excluir"
                                                onClick={() => handleDeleteStudent(student)}
                                            >
                                                <i className="bi bi-trash" />
                                            </Button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            {!isLoading && students.length === 0 && (
                                <tr>
                                    <td colSpan={6}>
                                        <div className="empty-state">
                                            <i className="bi bi-people" />
                                            Nenhum estudante cadastrado ainda.
                                        </div>
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </Table>
                )}
            </section>
        </>
    )
}

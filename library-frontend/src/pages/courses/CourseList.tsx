import { useCallback, useEffect, useMemo, useState } from "react"
import { Badge, Button, Spinner, Table } from "react-bootstrap"
import Swal from "sweetalert2"
import PageHeader from "../../components/PageHeader"
import StatCard from "../../components/StatCard"
import { createCourse, deleteCourse, getCourseSummary, updateCourse } from "../../services/courses-api"
import {
    DEGREE_LEVELS,
    DEGREE_LEVEL_LABELS,
    type CourseInput,
    type CourseSummary,
    type DegreeLevel,
} from "../../types/courses"
import { errorMessage, escapeHtml } from "../../utils/format"

const levelOptions = (selected: DegreeLevel) =>
    DEGREE_LEVELS.map(
        (level) =>
            `<option value="${level}" ${level === selected ? "selected" : ""}>${
                DEGREE_LEVEL_LABELS[level]
            }</option>`,
    ).join("")

const readForm = (): CourseInput | null => {
    const value = (id: string) => (document.getElementById(id) as HTMLInputElement | null)?.value ?? ""

    const name = value("course-name").trim()
    const code = value("course-code").trim()
    const department = value("course-department").trim()
    const durationSemesters = Number(value("course-duration"))
    const degreeLevel = value("course-level") as DegreeLevel

    if (!name || !code) {
        Swal.showValidationMessage("Nome e código são obrigatórios.")
        return null
    }

    if (!durationSemesters || durationSemesters < 1 || durationSemesters > 20) {
        Swal.showValidationMessage("A duração deve ficar entre 1 e 20 semestres.")
        return null
    }

    return {
        name,
        code,
        degreeLevel: degreeLevel || "GRADUACAO",
        durationSemesters,
        department: department || null,
    }
}

const formHtml = (course?: CourseSummary) => `
    <input id="course-name" class="swal2-input" placeholder="Nome do curso" value="${escapeHtml(course?.name ?? "")}">
    <input id="course-code" class="swal2-input" placeholder="Código (ex.: ESW)" value="${escapeHtml(course?.code ?? "")}">
    <label class="swal-form-label" for="course-level">Nível</label>
    <select id="course-level" class="swal2-select">${levelOptions(course?.degreeLevel ?? "GRADUACAO")}</select>
    <input id="course-duration" class="swal2-input" placeholder="Duração em semestres" type="number" min="1" max="20" value="${
        course?.durationSemesters ?? 8
    }">
    <input id="course-department" class="swal2-input" placeholder="Departamento" value="${escapeHtml(
        course?.department ?? "",
    )}">
`

export default function CourseList() {
    const [courses, setCourses] = useState<CourseSummary[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const fetchData = useCallback(async () => {
        try {
            setCourses(await getCourseSummary())
            setError(null)
        } catch (err) {
            setError(
                `${errorMessage(err, "Não foi possível carregar os cursos.")} - verifique se a students-api está no ar.`,
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

    const totalActiveStudents = useMemo(
        () => courses.reduce((sum, course) => sum + course.activeStudents, 0),
        [courses],
    )
    const longestCourse = useMemo(
        () => courses.reduce((max, course) => Math.max(max, course.durationSemesters), 0),
        [courses],
    )

    const handleAddCourse = async () => {
        const result = await Swal.fire({
            title: "Novo curso",
            html: formHtml(),
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
            await createCourse(result.value)
            await load()
            await Swal.fire("Salvo", "Curso cadastrado com sucesso.", "success")
        } catch (err) {
            await Swal.fire("Erro", errorMessage(err, "Não foi possível salvar o curso."), "error")
        }
    }

    const handleEditCourse = async (course: CourseSummary) => {
        const result = await Swal.fire({
            title: "Editar curso",
            html: formHtml(course),
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
            await updateCourse(course.id, result.value)
            await load()
            await Swal.fire("Atualizado", "Curso atualizado com sucesso.", "success")
        } catch (err) {
            await Swal.fire("Erro", errorMessage(err, "Não foi possível atualizar o curso."), "error")
        }
    }

    const handleDeleteCourse = async (course: CourseSummary) => {
        const result = await Swal.fire({
            title: "Remover curso?",
            text: `Isso vai excluir "${course.name}".`,
            icon: "warning",
            showCancelButton: true,
            confirmButtonText: "Excluir",
            cancelButtonText: "Cancelar",
        })

        if (!result.isConfirmed) {
            return
        }

        try {
            await deleteCourse(course.id)
            await load()
            await Swal.fire("Excluído", "Curso removido com sucesso.", "success")
        } catch (err) {
            // 409 quando o curso ainda tem alunos
            await Swal.fire("Não foi possível excluir", errorMessage(err, "Erro ao remover o curso."), "error")
        }
    }

    return (
        <>
            <PageHeader
                eyebrow="students-api"
                title="Cursos"
                description="Cursos oferecidos e quantos alunos ativos cada um tem."
                actions={
                    <Button variant="primary" onClick={handleAddCourse}>
                        <i className="bi bi-mortarboard me-2" />
                        Novo curso
                    </Button>
                }
            />

            <div className="stat-grid">
                <StatCard label="Cursos" value={courses.length} icon="bi-mortarboard" isLoading={isLoading} />
                <StatCard
                    label="Alunos ativos"
                    value={totalActiveStudents}
                    icon="bi-person-check"
                    tone="success"
                    isLoading={isLoading}
                />
                <StatCard
                    label="Maior duração"
                    value={longestCourse ? `${longestCourse} semestres` : "-"}
                    icon="bi-hourglass-split"
                    tone="neutral"
                    isLoading={isLoading}
                />
            </div>

            <section className="data-card">
                <div className="data-card-header">
                    <div>
                        <h2 className="data-card-title">Catálogo de cursos</h2>
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
                                <th>Curso</th>
                                <th>Nível</th>
                                <th>Duração</th>
                                <th>Departamento</th>
                                <th className="text-center">Alunos ativos</th>
                                <th className="text-end">Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {courses.map((course) => (
                                <tr key={course.id}>
                                    <td>
                                        <span className="cell-main">{course.name}</span>
                                        <span className="cell-sub">{course.code}</span>
                                    </td>
                                    <td>{DEGREE_LEVEL_LABELS[course.degreeLevel]}</td>
                                    <td>{course.durationSemesters} semestres</td>
                                    <td>{course.department ?? "-"}</td>
                                    <td className="text-center">
                                        <Badge bg={course.activeStudents > 0 ? "primary" : "secondary"}>
                                            {course.activeStudents}
                                        </Badge>
                                    </td>
                                    <td className="text-end">
                                        <div className="row-actions">
                                            <Button
                                                variant="outline-secondary"
                                                size="sm"
                                                className="icon-btn"
                                                title="Editar"
                                                onClick={() => handleEditCourse(course)}
                                            >
                                                <i className="bi bi-pencil" />
                                            </Button>
                                            <Button
                                                variant="outline-danger"
                                                size="sm"
                                                className="icon-btn"
                                                title="Excluir"
                                                onClick={() => handleDeleteCourse(course)}
                                            >
                                                <i className="bi bi-trash" />
                                            </Button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            {!isLoading && courses.length === 0 && (
                                <tr>
                                    <td colSpan={6}>
                                        <div className="empty-state">
                                            <i className="bi bi-mortarboard" />
                                            Nenhum curso cadastrado ainda.
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

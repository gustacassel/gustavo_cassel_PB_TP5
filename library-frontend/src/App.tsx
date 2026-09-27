import { BrowserRouter, Route, Routes } from "react-router-dom"
import AppLayout from "./components/AppLayout"
import BookList from "./pages/books/BookList"
import CourseList from "./pages/courses/CourseList"
import Home from "./pages/home/Home"
import LoanList from "./pages/loans/LoanList"
import StudentList from "./pages/students/StudentList"

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route element={<AppLayout />}>
                    <Route path="/" element={<Home />} />
                    <Route path="/loans" element={<LoanList />} />
                    <Route path="/books" element={<BookList />} />
                    <Route path="/students" element={<StudentList />} />
                    <Route path="/courses" element={<CourseList />} />
                </Route>
            </Routes>
        </BrowserRouter>
    )
}

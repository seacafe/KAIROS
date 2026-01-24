import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Header } from './Header'

/**
 * 메인 레이아웃 - Sidebar + Header + Content
 */
export function MainLayout() {
    return (
        <div className="flex h-screen bg-background">
            {/* 사이드바 */}
            <Sidebar />

            {/* 메인 콘텐츠 영역 */}
            <div className="flex flex-1 flex-col overflow-hidden">
                {/* 헤더 */}
                <Header />

                {/* 페이지 콘텐츠 */}
                <main className="flex-1 overflow-auto p-6">
                    <Outlet />
                </main>
            </div>
        </div>
    )
}

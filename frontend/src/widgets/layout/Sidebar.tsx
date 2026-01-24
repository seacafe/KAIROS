import { NavLink } from 'react-router-dom'
import {
    LayoutDashboard,
    BookOpen,
    Settings,
    Bot,
    BarChart3
} from 'lucide-react'

const navItems = [
    { path: '/', icon: LayoutDashboard, label: 'Dashboard' },
    { path: '/analysis', icon: BarChart3, label: 'Deep Analysis' },
    { path: '/journal', icon: BookOpen, label: 'Journal' },
    { path: '/agents', icon: Bot, label: 'Agents' },
    { path: '/settings', icon: Settings, label: 'Settings' },
]

/**
 * 사이드바 네비게이션
 */
export function Sidebar() {
    return (
        <aside className="flex w-64 flex-col border-r border-border bg-card">
            {/* 로고 */}
            <div className="flex h-16 items-center border-b border-border px-6">
                <h1 className="text-xl font-bold text-primary">KAIROS</h1>
                <span className="ml-2 text-xs text-muted-foreground">v0.1.0</span>
            </div>

            {/* 네비게이션 */}
            <nav className="flex-1 space-y-1 p-4">
                {navItems.map((item) => (
                    <NavLink
                        key={item.path}
                        to={item.path}
                        className={({ isActive }) =>
                            `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${isActive
                                ? 'bg-primary text-primary-foreground'
                                : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                            }`
                        }
                    >
                        <item.icon className="h-5 w-5" />
                        {item.label}
                    </NavLink>
                ))}
            </nav>

            {/* 하단 정보 */}
            <div className="border-t border-border p-4">
                <div className="text-xs text-muted-foreground">
                    7-Agent Trading System
                </div>
            </div>
        </aside>
    )
}

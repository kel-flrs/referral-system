'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { motion } from 'framer-motion';
import {
  LayoutDashboard,
  GitCompare,
  Send,
  Settings,
  Sparkles,
  ChevronLeft,
  Search,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { useState } from 'react';

const navItems = [
  { href: '/', label: 'Overview', icon: LayoutDashboard },
  { href: '/matches', label: 'Matches', icon: GitCompare },
  { href: '/referrals', label: 'Referrals', icon: Send },
  { href: '/settings', label: 'Settings', icon: Settings },
];

export function Sidebar() {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);

  return (
    <motion.aside
      initial={false}
      animate={{ width: collapsed ? 72 : 260 }}
      transition={{ duration: 0.2, ease: 'easeInOut' }}
      className="fixed left-0 top-0 z-40 flex h-screen flex-col border-r border-sidebar-border bg-sidebar"
    >
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 border-b border-sidebar-border px-4">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10">
          <Sparkles className="h-5 w-5 text-primary" />
        </div>
        {!collapsed && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="flex flex-col"
          >
            <span className="text-sm font-semibold text-sidebar-foreground">
              RefMatch
            </span>
            <span className="text-[10px] text-muted-foreground">
              Consultant Portal
            </span>
          </motion.div>
        )}
      </div>

      {/* Search trigger */}
      {!collapsed && (
        <div className="px-3 py-4">
          <button className="flex w-full items-center gap-2 rounded-lg border border-sidebar-border bg-sidebar-accent/50 px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-sidebar-accent hover:text-sidebar-foreground">
            <Search className="h-4 w-4" />
            <span>Search...</span>
            <kbd className="ml-auto rounded bg-sidebar-accent px-1.5 py-0.5 text-[10px] font-medium">
              ⌘K
            </kbd>
          </button>
        </div>
      )}

      {/* Navigation */}
      <nav className="flex-1 space-y-1 px-3 py-2">
        {navItems.map((item) => {
          const isActive = pathname === item.href ||
            (item.href !== '/' && pathname.startsWith(item.href));

          return (
            <Link key={item.href} href={item.href}>
              <motion.div
                className={cn(
                  'group relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'text-sidebar-primary-foreground'
                    : 'text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground'
                )}
                whileHover={{ x: 2 }}
                whileTap={{ scale: 0.98 }}
              >
                {isActive && (
                  <motion.div
                    layoutId="activeNav"
                    className="absolute inset-0 rounded-lg bg-sidebar-primary"
                    transition={{ type: 'spring', bounce: 0.2, duration: 0.4 }}
                  />
                )}
                <item.icon className={cn('relative z-10 h-4 w-4', isActive && 'text-sidebar-primary-foreground')} />
                {!collapsed && (
                  <span className="relative z-10">{item.label}</span>
                )}
              </motion.div>
            </Link>
          );
        })}
      </nav>

      {/* Collapse toggle */}
      <div className="border-t border-sidebar-border p-3">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => setCollapsed(!collapsed)}
          className="w-full justify-center text-muted-foreground hover:text-sidebar-foreground"
        >
          <motion.div
            animate={{ rotate: collapsed ? 180 : 0 }}
            transition={{ duration: 0.2 }}
          >
            <ChevronLeft className="h-4 w-4" />
          </motion.div>
        </Button>
      </div>
    </motion.aside>
  );
}

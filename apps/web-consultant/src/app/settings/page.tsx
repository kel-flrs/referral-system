'use client';

import { motion } from 'framer-motion';
import { Settings, Bell, Palette, Shield, User } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Separator } from '@/components/ui/separator';

export default function SettingsPage() {
  return (
    <div className="space-y-6">
      {/* Page header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-1"
      >
        <h1 className="flex items-center gap-2 text-2xl font-bold tracking-tight">
          <Settings className="h-6 w-6 text-muted-foreground" />
          Settings
        </h1>
        <p className="text-muted-foreground">
          Manage your account and preferences
        </p>
      </motion.div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Profile */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <User className="h-4 w-4" />
              Profile
            </CardTitle>
            <CardDescription>Your personal information</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <label className="text-sm font-medium">First Name</label>
                <Input defaultValue="John" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Last Name</label>
                <Input defaultValue="Doe" />
              </div>
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Email</label>
              <Input type="email" defaultValue="john.doe@company.com" />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Department</label>
              <Input defaultValue="Technical Recruiting" />
            </div>
            <Separator />
            <Button>Save Changes</Button>
          </CardContent>
        </Card>

        {/* Quick settings */}
        <div className="space-y-4">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <Bell className="h-4 w-4" />
                Notifications
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                Configure email and push notifications for new matches and referral updates.
              </p>
              <Button variant="outline" size="sm" className="mt-4">
                Configure
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <Palette className="h-4 w-4" />
                Appearance
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                Customize the dashboard theme and display preferences.
              </p>
              <Button variant="outline" size="sm" className="mt-4">
                Customize
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="flex items-center gap-2 text-base">
                <Shield className="h-4 w-4" />
                Security
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                Manage password and two-factor authentication settings.
              </p>
              <Button variant="outline" size="sm" className="mt-4">
                Manage
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

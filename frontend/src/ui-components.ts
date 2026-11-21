import type { App } from 'vue'
import StatusBadge from './components/StatusBadge.vue'
import ThemeToggle from './components/ThemeToggle.vue'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator } from '@/components/ui/breadcrumb'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { Combobox, ComboboxAnchor, ComboboxEmpty, ComboboxInput, ComboboxItem, ComboboxList, ComboboxViewport } from '@/components/ui/combobox'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationNext, PaginationPrevious } from '@/components/ui/pagination'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Separator } from '@/components/ui/separator'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Sidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupLabel, SidebarHeader, SidebarInset, SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarMenuSub, SidebarProvider, SidebarRail, SidebarTrigger } from '@/components/ui/sidebar'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'

const ui = {
  UiAlert: Alert,
  UiAlertDescription: AlertDescription,
  UiAlertTitle: AlertTitle,
  UiAvatar: Avatar,
  UiAvatarFallback: AvatarFallback,
  UiBadge: Badge,
  UiBreadcrumb: Breadcrumb,
  UiBreadcrumbItem: BreadcrumbItem,
  UiBreadcrumbLink: BreadcrumbLink,
  UiBreadcrumbList: BreadcrumbList,
  UiBreadcrumbPage: BreadcrumbPage,
  UiBreadcrumbSeparator: BreadcrumbSeparator,
  UiButton: Button,
  UiCard: Card,
  UiCardContent: CardContent,
  UiCardDescription: CardDescription,
  UiCardHeader: CardHeader,
  UiCheckbox: Checkbox,
  UiCollapsible: Collapsible,
  UiCollapsibleContent: CollapsibleContent,
  UiCollapsibleTrigger: CollapsibleTrigger,
  UiCombobox: Combobox,
  UiComboboxAnchor: ComboboxAnchor,
  UiComboboxEmpty: ComboboxEmpty,
  UiComboboxInput: ComboboxInput,
  UiComboboxItem: ComboboxItem,
  UiComboboxList: ComboboxList,
  UiComboboxViewport: ComboboxViewport,
  UiDialog: Dialog,
  UiDialogContent: DialogContent,
  UiDialogDescription: DialogDescription,
  UiDialogFooter: DialogFooter,
  UiDialogHeader: DialogHeader,
  UiDialogTitle: DialogTitle,
  UiDropdownMenu: DropdownMenu,
  UiDropdownMenuContent: DropdownMenuContent,
  UiDropdownMenuItem: DropdownMenuItem,
  UiDropdownMenuLabel: DropdownMenuLabel,
  UiDropdownMenuSeparator: DropdownMenuSeparator,
  UiDropdownMenuTrigger: DropdownMenuTrigger,
  UiField: Field,
  UiFieldError: FieldError,
  UiFieldGroup: FieldGroup,
  UiFieldLabel: FieldLabel,
  UiInput: Input,
  UiPagination: Pagination,
  UiPaginationContent: PaginationContent,
  UiPaginationEllipsis: PaginationEllipsis,
  UiPaginationItem: PaginationItem,
  UiPaginationNext: PaginationNext,
  UiPaginationPrevious: PaginationPrevious,
  UiRadioGroup: RadioGroup,
  UiRadioGroupItem: RadioGroupItem,
  UiSeparator: Separator,
  UiSheet: Sheet,
  UiSheetContent: SheetContent,
  UiSheetDescription: SheetDescription,
  UiSheetFooter: SheetFooter,
  UiSheetHeader: SheetHeader,
  UiSheetTitle: SheetTitle,
  UiSidebar: Sidebar,
  UiSidebarContent: SidebarContent,
  UiSidebarFooter: SidebarFooter,
  UiSidebarGroup: SidebarGroup,
  UiSidebarGroupLabel: SidebarGroupLabel,
  UiSidebarHeader: SidebarHeader,
  UiSidebarInset: SidebarInset,
  UiSidebarMenu: SidebarMenu,
  UiSidebarMenuButton: SidebarMenuButton,
  UiSidebarMenuItem: SidebarMenuItem,
  UiSidebarMenuSub: SidebarMenuSub,
  UiSidebarProvider: SidebarProvider,
  UiSidebarRail: SidebarRail,
  UiSidebarTrigger: SidebarTrigger,
  UiSkeleton: Skeleton,
  UiTable: Table,
  UiTableBody: TableBody,
  UiTableCaption: TableCaption,
  UiTableCell: TableCell,
  UiTableHead: TableHead,
  UiTableHeader: TableHeader,
  UiTableRow: TableRow,
  UiTextarea: Textarea,
  StatusBadge,
  ThemeToggle,
}

type RegisteredComponents = typeof ui

declare module 'vue' {
  interface GlobalComponents extends RegisteredComponents {}
}

export function installComponents(app: App) {
  for (const [name, component] of Object.entries(ui)) app.component(name, component)
}

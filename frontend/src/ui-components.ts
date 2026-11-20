import type { App } from 'vue'
import StatusBadge from './components/StatusBadge.vue'
import ThemeToggle from './components/ThemeToggle.vue'
import { Alert, AlertAction, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Avatar, AvatarBadge, AvatarFallback, AvatarGroup, AvatarGroupCount, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Breadcrumb, BreadcrumbEllipsis, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator } from '@/components/ui/breadcrumb'
import { Button } from '@/components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { Combobox, ComboboxAnchor, ComboboxEmpty, ComboboxGroup, ComboboxInput, ComboboxItem, ComboboxItemIndicator, ComboboxList, ComboboxSeparator, ComboboxTrigger, ComboboxViewport } from '@/components/ui/combobox'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogOverlay, DialogScrollContent, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { DropdownMenu, DropdownMenuCheckboxItem, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuLabel, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuSeparator, DropdownMenuShortcut, DropdownMenuSub, DropdownMenuSubContent, DropdownMenuSubTrigger, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Field, FieldContent, FieldDescription, FieldError, FieldGroup, FieldLabel, FieldLegend, FieldSeparator, FieldSet, FieldTitle } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { InputGroup, InputGroupAddon, InputGroupButton, InputGroupInput, InputGroupText, InputGroupTextarea } from '@/components/ui/input-group'
import { Label } from '@/components/ui/label'
import { Pagination, PaginationContent, PaginationEllipsis, PaginationFirst, PaginationItem, PaginationLast, PaginationLink, PaginationNext, PaginationPrevious } from '@/components/ui/pagination'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Separator } from '@/components/ui/separator'
import { Sheet, SheetClose, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { Sidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupAction, SidebarGroupContent, SidebarGroupLabel, SidebarHeader, SidebarInput, SidebarInset, SidebarMenu, SidebarMenuAction, SidebarMenuBadge, SidebarMenuButton, SidebarMenuItem, SidebarMenuSkeleton, SidebarMenuSub, SidebarMenuSubButton, SidebarMenuSubItem, SidebarProvider, SidebarRail, SidebarSeparator, SidebarTrigger } from '@/components/ui/sidebar'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCaption, TableCell, TableEmpty, TableFooter, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'

export const ui = {
  UiAlert: Alert,
  UiAlertAction: AlertAction,
  UiAlertDescription: AlertDescription,
  UiAlertTitle: AlertTitle,
  UiAvatar: Avatar,
  UiAvatarBadge: AvatarBadge,
  UiAvatarFallback: AvatarFallback,
  UiAvatarGroup: AvatarGroup,
  UiAvatarGroupCount: AvatarGroupCount,
  UiAvatarImage: AvatarImage,
  UiBadge: Badge,
  UiBreadcrumb: Breadcrumb,
  UiBreadcrumbEllipsis: BreadcrumbEllipsis,
  UiBreadcrumbItem: BreadcrumbItem,
  UiBreadcrumbLink: BreadcrumbLink,
  UiBreadcrumbList: BreadcrumbList,
  UiBreadcrumbPage: BreadcrumbPage,
  UiBreadcrumbSeparator: BreadcrumbSeparator,
  UiButton: Button,
  UiCard: Card,
  UiCardAction: CardAction,
  UiCardContent: CardContent,
  UiCardDescription: CardDescription,
  UiCardFooter: CardFooter,
  UiCardHeader: CardHeader,
  UiCardTitle: CardTitle,
  UiCheckbox: Checkbox,
  UiCollapsible: Collapsible,
  UiCollapsibleContent: CollapsibleContent,
  UiCollapsibleTrigger: CollapsibleTrigger,
  UiCombobox: Combobox,
  UiComboboxAnchor: ComboboxAnchor,
  UiComboboxEmpty: ComboboxEmpty,
  UiComboboxGroup: ComboboxGroup,
  UiComboboxInput: ComboboxInput,
  UiComboboxItem: ComboboxItem,
  UiComboboxItemIndicator: ComboboxItemIndicator,
  UiComboboxList: ComboboxList,
  UiComboboxSeparator: ComboboxSeparator,
  UiComboboxTrigger: ComboboxTrigger,
  UiComboboxViewport: ComboboxViewport,
  UiDialog: Dialog,
  UiDialogClose: DialogClose,
  UiDialogContent: DialogContent,
  UiDialogDescription: DialogDescription,
  UiDialogFooter: DialogFooter,
  UiDialogHeader: DialogHeader,
  UiDialogOverlay: DialogOverlay,
  UiDialogScrollContent: DialogScrollContent,
  UiDialogTitle: DialogTitle,
  UiDialogTrigger: DialogTrigger,
  UiDropdownMenu: DropdownMenu,
  UiDropdownMenuCheckboxItem: DropdownMenuCheckboxItem,
  UiDropdownMenuContent: DropdownMenuContent,
  UiDropdownMenuGroup: DropdownMenuGroup,
  UiDropdownMenuItem: DropdownMenuItem,
  UiDropdownMenuLabel: DropdownMenuLabel,
  UiDropdownMenuRadioGroup: DropdownMenuRadioGroup,
  UiDropdownMenuRadioItem: DropdownMenuRadioItem,
  UiDropdownMenuSeparator: DropdownMenuSeparator,
  UiDropdownMenuShortcut: DropdownMenuShortcut,
  UiDropdownMenuSub: DropdownMenuSub,
  UiDropdownMenuSubContent: DropdownMenuSubContent,
  UiDropdownMenuSubTrigger: DropdownMenuSubTrigger,
  UiDropdownMenuTrigger: DropdownMenuTrigger,
  UiField: Field,
  UiFieldContent: FieldContent,
  UiFieldDescription: FieldDescription,
  UiFieldError: FieldError,
  UiFieldGroup: FieldGroup,
  UiFieldLabel: FieldLabel,
  UiFieldLegend: FieldLegend,
  UiFieldSeparator: FieldSeparator,
  UiFieldSet: FieldSet,
  UiFieldTitle: FieldTitle,
  UiInput: Input,
  UiInputGroup: InputGroup,
  UiInputGroupAddon: InputGroupAddon,
  UiInputGroupButton: InputGroupButton,
  UiInputGroupInput: InputGroupInput,
  UiInputGroupText: InputGroupText,
  UiInputGroupTextarea: InputGroupTextarea,
  UiLabel: Label,
  UiPagination: Pagination,
  UiPaginationContent: PaginationContent,
  UiPaginationEllipsis: PaginationEllipsis,
  UiPaginationFirst: PaginationFirst,
  UiPaginationItem: PaginationItem,
  UiPaginationLast: PaginationLast,
  UiPaginationLink: PaginationLink,
  UiPaginationNext: PaginationNext,
  UiPaginationPrevious: PaginationPrevious,
  UiRadioGroup: RadioGroup,
  UiRadioGroupItem: RadioGroupItem,
  UiSeparator: Separator,
  UiSheet: Sheet,
  UiSheetClose: SheetClose,
  UiSheetContent: SheetContent,
  UiSheetDescription: SheetDescription,
  UiSheetFooter: SheetFooter,
  UiSheetHeader: SheetHeader,
  UiSheetTitle: SheetTitle,
  UiSheetTrigger: SheetTrigger,
  UiSidebar: Sidebar,
  UiSidebarContent: SidebarContent,
  UiSidebarFooter: SidebarFooter,
  UiSidebarGroup: SidebarGroup,
  UiSidebarGroupAction: SidebarGroupAction,
  UiSidebarGroupContent: SidebarGroupContent,
  UiSidebarGroupLabel: SidebarGroupLabel,
  UiSidebarHeader: SidebarHeader,
  UiSidebarInput: SidebarInput,
  UiSidebarInset: SidebarInset,
  UiSidebarMenu: SidebarMenu,
  UiSidebarMenuAction: SidebarMenuAction,
  UiSidebarMenuBadge: SidebarMenuBadge,
  UiSidebarMenuButton: SidebarMenuButton,
  UiSidebarMenuItem: SidebarMenuItem,
  UiSidebarMenuSkeleton: SidebarMenuSkeleton,
  UiSidebarMenuSub: SidebarMenuSub,
  UiSidebarMenuSubButton: SidebarMenuSubButton,
  UiSidebarMenuSubItem: SidebarMenuSubItem,
  UiSidebarProvider: SidebarProvider,
  UiSidebarRail: SidebarRail,
  UiSidebarSeparator: SidebarSeparator,
  UiSidebarTrigger: SidebarTrigger,
  UiSkeleton: Skeleton,
  UiTable: Table,
  UiTableBody: TableBody,
  UiTableCaption: TableCaption,
  UiTableCell: TableCell,
  UiTableEmpty: TableEmpty,
  UiTableFooter: TableFooter,
  UiTableHead: TableHead,
  UiTableHeader: TableHeader,
  UiTableRow: TableRow,
  UiTextarea: Textarea,
  UiTooltip: Tooltip,
  UiTooltipContent: TooltipContent,
  UiTooltipProvider: TooltipProvider,
  UiTooltipTrigger: TooltipTrigger,
  StatusBadge,
  ThemeToggle,
}

type RegisteredComponents = typeof ui

declare module 'vue' {
  interface GlobalComponents extends RegisteredComponents {}
}

export function installComponents(app: App, components: RegisteredComponents) {
  for (const [name, component] of Object.entries(components)) app.component(name, component)
}

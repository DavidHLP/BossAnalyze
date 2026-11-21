import {
  ChartHistogramIcon,
  ComputerTerminal01Icon,
  File01Icon,
  MenuSquareIcon,
  Settings02Icon,
  ShieldUserIcon,
  UserCircleIcon,
  UserGroupIcon,
} from '@hugeicons/core-free-icons'

const icons: Record<string, typeof File01Icon> = {
  '/console': ComputerTerminal01Icon,
  '/console/dashboard': ChartHistogramIcon,
  '/console/users': UserGroupIcon,
  '/console/roles': ShieldUserIcon,
  '/console/menus': MenuSquareIcon,
  '/console/settings': Settings02Icon,
  '/console/profile': UserCircleIcon,
}
const menuIconNames: Record<string, typeof File01Icon> = {
  ChartHistogramIcon,
  ComputerTerminal01Icon,
  File01Icon,
  MenuSquareIcon,
  Settings02Icon,
  ShieldUserIcon,
  UserCircleIcon,
  UserGroupIcon,
}
export function menuIcon(path: string | null, name?: string) {
  if (name && Object.hasOwn(menuIconNames, name)) return menuIconNames[name]!
  return icons[path ?? ''] ?? File01Icon
}

import { Link } from 'react-router-dom'
import { LoaderCircle } from 'lucide-react'

const VARIANTS = {
  primary: 'btn-primary',
  secondary: 'btn-secondary',
  ghost: 'btn-ghost',
  soft: 'btn-soft',
  danger: 'btn-danger',
  success: 'btn-success',
}
const SIZES = { sm: 'btn-sm', md: '', lg: 'btn-lg' }

export default function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  icon: Icon,
  to,
  href,
  className = '',
  children,
  disabled,
  type = 'button',
  ...rest
}) {
  const classes = `btn ${VARIANTS[variant]} ${SIZES[size]} ${className}`
  const iconSize = size === 'sm' ? 14 : 16
  const content = (
    <>
      {loading ? <LoaderCircle size={iconSize} className="animate-spin" /> : Icon && <Icon size={iconSize} />}
      {children}
    </>
  )
  if (to) {
    return (
      <Link to={to} className={classes} {...rest}>
        {content}
      </Link>
    )
  }
  if (href) {
    return (
      <a href={href} className={classes} target="_blank" rel="noopener noreferrer" {...rest}>
        {content}
      </a>
    )
  }
  return (
    <button type={type} className={classes} disabled={disabled || loading} {...rest}>
      {content}
    </button>
  )
}

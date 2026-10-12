/**
 * Small, safe Markdown renderer for chat messages. Supports **bold**, *italic* / _italic_, ~~strike~~, `code`,
 * ```code blocks```, "- " bullet lists, [links](https://…) and bare https:// URLs. Everything is built as React
 * elements (never raw HTML), and only http(s) links are allowed, so message text cannot inject markup or scripts.
 * Plain text without Markdown renders exactly as before.
 */

const INLINE = new RegExp(
  [
    '(?<code>`[^`\\n]+`)',
    '(?<bold>\\*\\*(?!\\s)[^*\\n]+?(?<!\\s)\\*\\*)',
    '(?<strike>~~(?!\\s)[^~\\n]+?(?<!\\s)~~)',
    '(?<link>\\[[^\\]\\n]+\\]\\(https?:\\/\\/[^\\s)]+\\))',
    '(?<url>https?:\\/\\/[^\\s<]*[^\\s<.,:;"\'!?)\\]])',
    '(?<italic>(?<![\\w*])\\*(?!\\s)[^*\\n]+?(?<!\\s)\\*(?![\\w*]))',
    '(?<underscore>(?<![\\w])_(?!\\s)[^_\\n]+?(?<!\\s)_(?![\\w]))',
  ].join('|'),
  'g',
)

const linkClass = 'underline underline-offset-2 break-all hover:opacity-80'
const codeClass = 'rounded bg-black/10 px-1 py-0.5 font-mono text-[0.85em]'

function renderInline(text, keyBase) {
  const out = []
  let last = 0
  for (const match of text.matchAll(INLINE)) {
    if (match.index > last) out.push(text.slice(last, match.index))
    const key = `${keyBase}-${match.index}`
    const g = match.groups
    if (g.code) out.push(<code key={key} className={codeClass}>{g.code.slice(1, -1)}</code>)
    else if (g.bold) out.push(<strong key={key}>{renderInline(g.bold.slice(2, -2), key)}</strong>)
    else if (g.strike) out.push(<s key={key}>{renderInline(g.strike.slice(2, -2), key)}</s>)
    else if (g.link) {
      const [, label, href] = g.link.match(/^\[([^\]]+)\]\((.+)\)$/)
      out.push(<a key={key} href={href} target="_blank" rel="noopener noreferrer" className={linkClass}>{label}</a>)
    } else if (g.url) {
      out.push(<a key={key} href={g.url} target="_blank" rel="noopener noreferrer" className={linkClass}>{g.url}</a>)
    } else if (g.italic) out.push(<em key={key}>{renderInline(g.italic.slice(1, -1), key)}</em>)
    else if (g.underscore) out.push(<em key={key}>{renderInline(g.underscore.slice(1, -1), key)}</em>)
    last = match.index + match[0].length
  }
  if (last < text.length) out.push(text.slice(last))
  return out
}

const BULLET = /^\s*[-*•]\s+/

/** Splits text into code blocks, bullet lists and plain runs of lines. */
function toBlocks(text) {
  const blocks = []
  const parts = text.split(/```/)
  parts.forEach((part, i) => {
    // Odd parts sit between ``` fences; an unclosed fence is shown as plain text.
    if (i % 2 === 1 && i < parts.length - 1) {
      blocks.push({ type: 'code', text: part.replace(/^[a-z0-9+-]*\n/i, '').replace(/\n$/, '') })
      return
    }
    const raw = i % 2 === 1 ? '```' + part : part
    let current = null
    for (const line of raw.split('\n')) {
      const type = BULLET.test(line) ? 'list' : 'text'
      if (!current || current.type !== type) {
        current = { type, lines: [] }
        blocks.push(current)
      }
      current.lines.push(type === 'list' ? line.replace(BULLET, '') : line)
    }
  })
  return blocks
}

export default function Markdown({ text, className = '' }) {
  if (!text) return null
  const blocks = toBlocks(text)
  return (
    <div className={['break-words whitespace-pre-wrap', className].filter(Boolean).join(' ')}>
      {blocks.map((block, i) => {
        if (block.type === 'code') {
          return (
            <pre key={i} className="my-1 overflow-x-auto rounded-lg bg-black/10 px-2.5 py-2 font-mono text-xs whitespace-pre">
              {block.text}
            </pre>
          )
        }
        if (block.type === 'list') {
          return (
            <ul key={i} className="my-0.5 list-disc space-y-0.5 pl-5 whitespace-normal">
              {block.lines.map((line, j) => <li key={j}>{renderInline(line, `${i}-${j}`)}</li>)}
            </ul>
          )
        }
        // Text lines between other blocks keep their own line breaks; drop the empty edge left by a fence or list.
        let content = block.lines.join('\n')
        if (i > 0) content = content.replace(/^\n/, '')
        if (i < blocks.length - 1) content = content.replace(/\n$/, '')
        if (!content && blocks.length > 1) return null
        return <span key={i}>{renderInline(content, String(i))}</span>
      })}
    </div>
  )
}

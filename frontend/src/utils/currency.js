export function decimalToScaledInteger(value, decimalPlaces = 2) {
  const text = String(value).trim()
  const match = new RegExp(`^(\\d+)(?:\\.(\\d{1,${decimalPlaces}}))?$`).exec(text)
  if (!match) throw new Error(`Enter a value with at most ${decimalPlaces} decimal places`)
  const scale = 10n ** BigInt(decimalPlaces)
  const scaled = BigInt(match[1]) * scale + BigInt((match[2] || '').padEnd(decimalPlaces, '0') || '0')
  if (scaled > BigInt(Number.MAX_SAFE_INTEGER)) throw new Error('Amount is too large')
  return Number(scaled)
}

export const rupeesToPaise = value => decimalToScaledInteger(value, 2)

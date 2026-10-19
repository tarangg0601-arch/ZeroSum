import { decimalToScaledInteger } from './currency.js'

export function createSplitPayload(amountPaise, splitType, participantIds, values) {
  if (splitType === 'equal') return { splitType }
  const conversion = value => splitType === 'shares' ? Number(value) : decimalToScaledInteger(value, 2)
  const splits = participantIds.map(userId => {
    const value = conversion(values[userId])
    if (!Number.isFinite(value) || value < 0 || (splitType === 'shares' && !Number.isInteger(value))) {
      throw new Error('Enter a valid non-negative value for each participant.')
    }
    return { userId: Number(userId), value }
  })
  const total = splits.reduce((sum, split) => sum + split.value, 0)
  if (splitType === 'exact' && total !== amountPaise) throw new Error('Exact split amounts must equal the expense total.')
  if (splitType === 'percentage' && total !== 10000) throw new Error('Percentages must add up to 100%.')
  if (splitType === 'shares' && total <= 0) throw new Error('Enter at least one share unit.')
  return { splitType, splits }
}

import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { createSplitPayload } from './expenseSplits.js'

describe('createSplitPayload', () => {
  it('leaves equal allocation to the backend', () => {
    assert.deepEqual(createSplitPayload(100, 'equal', [1, 2], {}), { splitType: 'equal' })
  })

  it('converts exact rupees to paise and checks reconciliation', () => {
    assert.deepEqual(createSplitPayload(10001, 'exact', [1, 2], { 1: '60.01', 2: '40' }), {
      splitType: 'exact', splits: [{ userId: 1, value: 6001 }, { userId: 2, value: 4000 }],
    })
    assert.throws(() => createSplitPayload(10000, 'exact', [1], { 1: '99' }), /equal the expense total/)
  })

  it('converts percentages to basis points', () => {
    assert.deepEqual(createSplitPayload(101, 'percentage', [1, 2, 3], { 1: '33.33', 2: '33.33', 3: '33.34' }), {
      splitType: 'percentage', splits: [{ userId: 1, value: 3333 }, { userId: 2, value: 3333 }, { userId: 3, value: 3334 }],
    })
  })

  it('requires valid positive share weights', () => {
    assert.deepEqual(createSplitPayload(101, 'shares', [1, 2], { 1: '1', 2: '2' }).splits.map(item => item.value), [1, 2])
    assert.throws(() => createSplitPayload(101, 'shares', [1], { 1: '0' }), /at least one share unit/)
  })
})

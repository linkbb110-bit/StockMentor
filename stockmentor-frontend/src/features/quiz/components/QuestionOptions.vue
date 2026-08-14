<script setup lang="ts">
import type { QuestionType, QuizOption } from '../types/quiz'

const props = withDefaults(
  defineProps<{
    name: string
    type: QuestionType
    options: QuizOption[]
    selectedOptionIds: number[]
    disabled?: boolean
  }>(),
  { disabled: false },
)

const emit = defineEmits<{
  'update:selectedOptionIds': [value: number[]]
}>()

const isMultiple = (): boolean => props.type === 'MULTIPLE_CHOICE'

const selectOption = (optionId: number, selected: boolean): void => {
  if (!isMultiple()) {
    emit('update:selectedOptionIds', [optionId])
    return
  }

  const selectedIds = new Set(props.selectedOptionIds)
  if (selected) {
    selectedIds.add(optionId)
  } else {
    selectedIds.delete(optionId)
  }
  emit(
    'update:selectedOptionIds',
    props.options.filter((option) => selectedIds.has(option.optionId)).map((option) => option.optionId),
  )
}
</script>

<template>
  <div class="question-options">
    <label v-for="option in options" :key="option.optionId" class="option-row">
      <input
        :type="isMultiple() ? 'checkbox' : 'radio'"
        :name="name"
        :value="option.optionId"
        :checked="selectedOptionIds.includes(option.optionId)"
        :disabled="disabled"
        @change="selectOption(option.optionId, ($event.target as HTMLInputElement).checked)"
      />
      <span><strong>{{ option.optionKey }}.</strong> {{ option.content }}</span>
    </label>
  </div>
</template>

<style scoped>
.question-options {
  display: grid;
  gap: 0.65rem;
  margin-top: 1rem;
}

.option-row {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  padding: 0.75rem;
  border: 1px solid #dce4ef;
  border-radius: 0.65rem;
  color: #334155;
  cursor: pointer;
  line-height: 1.5;
}

.option-row:has(input:checked) {
  border-color: #3b82f6;
  background: #eff6ff;
}

.option-row input {
  margin-top: 0.25rem;
}

.option-row:has(input:disabled) {
  cursor: not-allowed;
  opacity: 0.7;
}
</style>

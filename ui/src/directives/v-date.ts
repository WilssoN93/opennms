import { format as fnsFormat } from 'date-fns-tz'
import { parseISO } from 'date-fns'
import { AppInfo } from '@/types'
import { useInfoStore } from '@/stores/infoStore'

const infoStore = computed(() => useInfoStore())
const appInfo = computed<AppInfo>(() => infoStore.value.info)

const timeZone = computed<string>(
  () => appInfo.value.datetimeformatConfig?.zoneId || Intl.DateTimeFormat().resolvedOptions().timeZone
)

const formatString = computed<string>(
  // eslint-disable-next-line quotes
  () => appInfo.value.datetimeformatConfig?.datetimeformat || "yyyy-MM-dd'T'HH:mm:ssxxx"
)

const dateFormatDirective = {
  mounted(el: Element) {
    if (!el.innerHTML) {
      return
    }

    const date = Number(el.innerHTML) || parseISO(el.innerHTML)

    if (!date) {
      return
    }

    const formattedDate = fnsFormat(date, formatString.value, { timeZone: timeZone.value })
    el.innerHTML = formattedDate
  }
}

export default dateFormatDirective

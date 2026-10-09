<template>
  <div class="workspace">
    <!-- 左侧：投放分组 -->
    <aside class="group-panel">
      <div class="group-head">
        <div>
          <div class="group-head-title">投放分组</div>
          <div class="group-head-sub">共 {{ editableTabs?.length || 0 }} 组 · 拖动可排序</div>
        </div>
        <el-tooltip content="新建分组" placement="bottom" :show-after="300">
          <button type="button" class="icon-btn primary" aria-label="新建分组" @click="showAddGroup">
            <el-icon><Plus /></el-icon>
          </button>
        </el-tooltip>
      </div>

      <!-- 拖动选项 -->
      <ul class="sortOptions">
        <li v-for="(item, index) in editableTabs" :key="item.gid">
          <div class="group-item" :class="{ selectedItem: selectedIndex === index }" @click="changeSelectIndex(index)">
            <el-icon class="drag-handle"><Rank /></el-icon>
            <span class="group-dot" :style="{ background: groupColor(index) }"></span>
            <span class="over-text group-name">{{ item.name }}</span>
            <div class="group-actions" @click.stop>
              <el-tooltip :show-after="500" content="分组数据" placement="bottom">
                <el-icon v-if="!(item.shortLinkCount === 0 || item.shortLinkCount === null)" class="edit"
                  @click.stop="chartsVisible({ describe: item.name, gid: item.gid, group: true })">
                  <Histogram />
                </el-icon>
              </el-tooltip>
              <el-dropdown trigger="click">
                <el-icon class="edit" v-if="item.title !== '默认分组'">
                  <MoreFilled />
                </el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :icon="EditPen" @click="showEditGroup(item.gid, item.name)">重命名</el-dropdown-item>
                    <el-dropdown-item :icon="Delete" @click="deleteGroup(item.gid)">删除分组</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <span class="item-length">{{ item.shortLinkCount ?? 0 }}</span>
          </div>
        </li>
      </ul>

      <div class="recycle-bin">
        <!-- 当selectIndex等于-1时代表选中的是回收站 -->
        <div class="group-item recycle-box" :class="{ selectedItem: selectedIndex === -1 }" @click="recycleBin">
          <el-icon class="recycle-icon"><Delete /></el-icon>
          <span class="group-name">回收站</span>
        </div>
      </div>
    </aside>

    <!-- 右侧：主要数据展示区域 -->
    <section class="content-box">
      <!-- 页面标题 + 操作 -->
      <div class="page-head">
        <div class="page-title-wrap">
          <template v-if="!isRecycleBin">
            <h1 class="page-title">{{ currentGroupName }}</h1>
            <p class="page-sub">管理该分组下的投放短链，查看点击、访客与渠道表现</p>
          </template>
          <template v-else>
            <h1 class="page-title">回收站</h1>
            <p class="page-sub">共 {{ recycleBinNums }} 条短链 · 可恢复，或彻底删除（不可逆）</p>
          </template>
        </div>
        <div v-if="!isRecycleBin" class="page-actions">
          <el-button :icon="DocumentCopy" @click="isAddSmallLinks = true">批量创建</el-button>
          <el-button class="addButton" type="primary" :icon="Plus" @click="isAddSmallLink = true">创建短链</el-button>
        </div>
      </div>

      <!-- 概览卡片（基于当前页数据汇总） -->
      <div v-if="!isRecycleBin" class="stat-cards">
        <div class="stat-card">
          <div class="stat-icon indigo"><el-icon><Link /></el-icon></div>
          <div>
            <div class="stat-label">短链总数</div>
            <div class="stat-value">{{ totalNums }}</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon teal"><el-icon><Pointer /></el-icon></div>
          <div>
            <div class="stat-label">今日点击 <span class="hint">本页</span></div>
            <div class="stat-value">{{ pageSum('todayPv') }}</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon amber"><el-icon><User /></el-icon></div>
          <div>
            <div class="stat-label">今日访客 <span class="hint">本页</span></div>
            <div class="stat-value">{{ pageSum('todayUv') }}</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon violet"><el-icon><TrendCharts /></el-icon></div>
          <div>
            <div class="stat-label">累计点击 <span class="hint">本页</span></div>
            <div class="stat-value">{{ pageSum('totalPv') }}</div>
          </div>
        </div>
      </div>

      <div class="table-box">
        <!-- 表格展示区域 -->
        <el-table :data="tableData" class="link-table" height="100%">
          <!-- 数据为空时展示的内容 -->
          <template #empty>
            <div class="empty-state">
              <div class="empty-icon"><el-icon><Link /></el-icon></div>
              <p class="empty-title">{{ isRecycleBin ? '回收站是空的' : '这个分组还没有短链' }}</p>
              <p class="empty-sub">{{ isRecycleBin ? '移入回收站的短链会出现在这里' : '创建第一条投放短链，开始追踪点击' }}</p>
              <el-button v-if="!isRecycleBin" type="primary" :icon="Plus" @click="isAddSmallLink = true">创建短链</el-button>
            </div>
          </template>
          <el-table-column type="selection" width="44" />
          <el-table-column label="短链接信息" prop="info" min-width="260">
            <template #header>
              <el-dropdown trigger="click">
                <div :class="{ orderIndex: orderIndex === 0 }" class="sort-head">
                  <span>短链接信息</span>
                  <el-icon><CaretBottom /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-item @click="pageParams.orderTag = null, orderIndex = 0">按创建时间</el-dropdown-item>
                </template>
              </el-dropdown>
            </template>
            <template #default="scope">
              <div class="table-link-box" :class="{
                isExpire: scope?.row?.validDateType === 1 && !isExpire(scope?.row?.validDate)
              }">
                <div class="favicon">
                  <img :src="getImgUrl(scope.row.favicon)" :key="scope?.row?.id" width="20" height="20" alt="" />
                </div>
                <div class="name-date">
                  <el-tooltip :show-after="500" :content="scope.row.describe">
                    <span class="link-title">{{ scope.row.describe }}</span>
                  </el-tooltip>
                  <div class="time">
                    <span>{{ scope.row.createTime }}</span>
                    <template v-if="scope?.row?.validDate">
                      <el-tooltip :show-after="300" :content="'到期时间：' + scope?.row?.validDate">
                        <span v-if="isExpire(scope?.row?.validDate)" class="badge badge-warn">
                          <el-icon><Timer /></el-icon>限时
                        </span>
                        <span v-else class="badge badge-danger">已失效</span>
                      </el-tooltip>
                    </template>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="短链接网址" prop="url" min-width="280">
            <template #default="scope">
              <div class="table-url-box">
                <!-- 当失效后就不能在点击跳转了 -->
                <el-link type="primary" :underline="false" target="_blank" class="short-url"
                  :disabled="scope?.row?.validDateType === 1 && !isExpire(scope?.row?.validDate)"
                  :href="'http://' + scope.row.fullShortUrl">{{ scope.row.domain + '/' + scope.row.shortUri }}</el-link>
                <el-tooltip :show-after="500" :content="scope.row.originUrl">
                  <span class="origin-url">{{ scope.row.originUrl }}</span>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="copy" width="96">
            <template #default="scope">
              <div class="quick-actions">
                <!-- 二维码 -->
                <QRCode :url="'http://' + scope.row.fullShortUrl"></QRCode>
                <!-- 表格中的复制链接按钮 -->
                <el-tooltip :show-after="300" content="复制链接" placement="bottom">
                  <button type="button" class="icon-btn" aria-label="复制链接"
                    @click="copyUrl('http://' + scope.row.fullShortUrl)">
                    <el-icon><CopyDocument /></el-icon>
                  </button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="访问次数" prop="times" width="120">
            <template #header>
              <el-dropdown trigger="click">
                <div :class="{ orderIndex: orderIndex === 1 }" class="sort-head">
                  <span>访问次数</span>
                  <el-icon><CaretBottom /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-item @click="pageParams.orderTag = 'todayPv', orderIndex = 1">今日访问次数</el-dropdown-item>
                  <el-dropdown-item @click="pageParams.orderTag = 'totalPv', orderIndex = 1">累计访问次数</el-dropdown-item>
                </template>
              </el-dropdown>
            </template>
            <template #default="scope">
              <div class="metric">
                <span class="metric-today">{{ scope.row.todayPv }}<em>今日</em></span>
                <span class="metric-total">累计 {{ scope.row.totalPv }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="访问人数" prop="people" width="120">
            <template #header>
              <el-dropdown trigger="click">
                <div :class="{ orderIndex: orderIndex === 2 }" class="sort-head">
                  <span>访问人数</span>
                  <el-icon><CaretBottom /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-item @click="pageParams.orderTag = 'todayUv', orderIndex = 2">今日访问人数</el-dropdown-item>
                  <el-dropdown-item @click="pageParams.orderTag = 'totalUv', orderIndex = 2">累计访问人数</el-dropdown-item>
                </template>
              </el-dropdown>
            </template>
            <template #default="scope">
              <div class="metric">
                <span class="metric-today">{{ scope.row.todayUv }}<em>今日</em></span>
                <span class="metric-total">累计 {{ scope.row.totalUv }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="IP数" prop="IP" width="110">
            <template #header>
              <el-dropdown trigger="click">
                <div :class="{ orderIndex: orderIndex === 3 }" class="sort-head">
                  <span>IP数</span>
                  <el-icon><CaretBottom /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-item @click="pageParams.orderTag = 'todayUip', orderIndex = 3">今日IP数</el-dropdown-item>
                  <el-dropdown-item @click="pageParams.orderTag = 'totalUip', orderIndex = 3">累计IP数</el-dropdown-item>
                </template>
              </el-dropdown>
            </template>
            <template #default="scope">
              <div class="metric">
                <span class="metric-today">{{ scope.row.todayUip }}<em>今日</em></span>
                <span class="metric-total">累计 {{ scope.row.totalUip }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column fixed="right" label="操作" width="148">
            <template #default="scope">
              <div class="row-actions">
                <el-tooltip :show-after="300" content="数据看板" placement="bottom">
                  <button type="button" class="icon-btn" aria-label="数据看板" @click="chartsVisible(scope.row)">
                    <el-icon><Histogram /></el-icon>
                  </button>
                </el-tooltip>
                <!-- 正常页面展示编辑和删除 -->
                <template v-if="selectedIndex !== -1">
                  <el-tooltip :show-after="300" content="编辑" placement="bottom">
                    <button type="button" class="icon-btn" aria-label="编辑" @click="editLink(scope.row)">
                      <el-icon><EditPen /></el-icon>
                    </button>
                  </el-tooltip>
                  <el-popconfirm width="180" title="确定移入回收站？" @confirm="toRecycleBin(scope.row)">
                    <template #reference>
                      <button type="button" class="icon-btn danger" aria-label="移入回收站">
                        <el-icon><Delete /></el-icon>
                      </button>
                    </template>
                  </el-popconfirm>
                </template>
                <!-- 回收站操作 -->
                <template v-else>
                  <el-tooltip :show-after="300" content="恢复" placement="bottom">
                    <button type="button" class="icon-btn" aria-label="恢复" @click="recoverLink(scope.row)">
                      <el-icon><RefreshLeft /></el-icon>
                    </button>
                  </el-tooltip>
                  <el-popconfirm width="300" title="删除后短链跳转会失效，同时停止数据统计，这是一个不可逆的操作，是否删除?"
                    @confirm="removeLink(scope.row)">
                    <template #reference>
                      <button type="button" class="icon-btn danger" aria-label="彻底删除">
                        <el-icon><Delete /></el-icon>
                      </button>
                    </template>
                  </el-popconfirm>
                </template>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <!-- 分页器 -->
        <div class="pagination-block">
          <el-pagination v-model:current-page="pageParams.current" v-model:page-size="pageParams.size"
            :page-sizes="[10, 15, 20, 30]" layout="total, sizes, prev, pager, next, jumper" :total="totalNums"
            background @size-change="handleSizeChange" @current-change="handleCurrentChange" />
        </div>
      </div>
    </section>

    <!-- 查看数据弹框 -->
    <ChartsInfo ref="chartsInfoRef" :title="chartsInfoTitle" :info="chartsInfo"
      :tableInfo="tableInfo" :isGroup="isGroup" :nums="nums" :favicon="favicon1" :originUrl="originUrl1"
      @changeTime="changeTime" @changePage="changePage"></ChartsInfo>
    <!-- 新建分组弹框 -->
    <el-dialog v-model="isAddGroup" title="新建投放分组" width="440px">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="分组名称">
          <el-input autocomplete="off" v-model="newGroupName" placeholder="如：618 大促 / 信息流-抖音" maxlength="20"
            show-word-limit @keyup.enter="addGroup" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="isAddGroup = false">取消</el-button>
          <el-button type="primary" @click="addGroup" :disabled="addGroupLoading">创建</el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 编辑分组弹框 -->
    <el-dialog v-model="isEditGroup" title="重命名分组" width="440px">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="分组名称">
          <el-input autocomplete="off" v-model="editGroupName" maxlength="20" show-word-limit
            @keyup.enter="editGroup" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="isEditGroup = false">取消</el-button>
          <el-button type="primary" @click="editGroup" :disabled="editGroupLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 创建短链弹框 -->
    <el-dialog @close="afterAddLink" v-model="isAddSmallLink" title="创建短链" width="600px">
      <el-tabs class="demo-tabs">
        <el-tab-pane>
          <template #label>
            <span class="custom-tabs-label">
              <el-icon><Link /></el-icon>
              <span>普通跳转</span>
            </span>
          </template>
          <CreateLink ref="createLink1Ref" :groupInfo="editableTabs" @onSubmit="addLink" @cancel="cancelAddLink"
            :defaultGid="pageParams.gid" :is-single="true"></CreateLink>
        </el-tab-pane>
        <el-tab-pane disabled>
          <template #label>
            <span class="custom-tabs-label">
              <el-icon><Connection /></el-icon>
              <span>随机跳转</span>
              <span class="soon">即将上线</span>
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
    <!-- 修改短链信息弹框 -->
    <el-dialog @close="afterAddLink" v-model="isEditLink" title="编辑短链" width="600px">
      <EditLink ref="editLinkRef" :editData="editData" :groupInfo="editableTabs" @onSubmit="coverEditLink"
        @updatePage="updatePage" @cancel="coverEditLink"></EditLink>
    </el-dialog>
    <!-- 批量创建短链弹框 -->
    <el-dialog @close="afterAddLink" v-model="isAddSmallLinks" title="批量创建短链" width="640px">
      <CreateLinks ref="createLink2Ref" :groupInfo="editableTabs" @onSubmit="addLink" @cancel="cancelAddLink"
        :defaultGid="pageParams.gid"></CreateLinks>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance, watch, nextTick } from 'vue'
import { Plus, Delete, EditPen, DocumentCopy } from '@element-plus/icons-vue'
import Sortable from 'sortablejs'
import { cloneDeep } from 'lodash'
import ChartsInfo from './components/chartsInfo/ChartsInfo.vue'
import CreateLink from './components/createLink/CreateLink.vue'
import CreateLinks from './components/createLink/CreateLinks.vue'
import { getTodayFormatDate, getLastWeekFormatDate } from '@/utils/plugins.js'
import EditLink from './components/editLink/EditLink.vue'
import { ElMessage } from 'element-plus'
import defaultImg from '@/assets/png/短链默认图标.png'
import QRCode from './components/qrCode/QRCode.vue'

// 查看图表的时候传过去展示的，没什么用
const nums = ref(0)
const favicon1 = ref()
const originUrl1 = ref()
const orderIndex = ref(0)

const { proxy } = getCurrentInstance()
const API = proxy.$API
const chartsInfoRef = ref()
const chartsInfoTitle = ref()
const chartsInfo = ref()
const tableInfo = ref()
const createLink1Ref = ref()
const createLink2Ref = ref()
let selectedIndex = ref(0)
const editableTabs = ref([])
// 添加弹窗关闭后重新请求一下页面数据
const afterAddLink = () => {
  setTimeout(() => {
    getGroupInfo(queryPage) // 重新请求数据
    // 按钮重新恢复可点击的样式
    document.querySelector('.addButton') && document.querySelector('.addButton').blur()
  }, 0)
  if (createLink1Ref.value) {
    createLink1Ref.value.initFormData()
  }
  if (createLink2Ref.value) {
    createLink2Ref.value.initFormData()
  }
  if (editLinkRef.value) {
    editLinkRef.value.initFormData()
  }
}
const statsFormData = reactive({
  endDate: getTodayFormatDate(),
  startDate: getLastWeekFormatDate(),
  size: 10,
  current: 1
})
const initStatsFormData = () => {
  statsFormData.endDate = getTodayFormatDate()
  statsFormData.startDate = getLastWeekFormatDate()
}
const visitLink = {
  fullShortUrl: '',
  gid: '',
  enableStatus: null
}
// 打开的图表是分组（true为分组）的还是单链的
const isGroup = ref(false)
const tableFullShortUrl = ref()
const tableGid = ref()
// 点击查看数据图表
const chartsVisible = async (rowInfo, dateList) => {
  chartsInfoTitle.value = rowInfo?.describe
  // 如果传入的group为true的话就查询分组的数据，如果没传就查询单链的数据
  const { fullShortUrl, gid, group, originUrl, favicon, enableStatus } = rowInfo
  originUrl1.value = originUrl
  favicon1.value = favicon
  isGroup.value = group
  tableFullShortUrl.value = fullShortUrl
  tableGid.value = gid
  // 后续修改时间的时候拿去用
  visitLink.fullShortUrl = fullShortUrl
  visitLink.gid = gid
  visitLink.enableStatus = enableStatus
  chartsInfoRef?.value.isVisible()
  // 如果没有时间传值，就默认查找过去一周的数据
  if (!dateList) {
    initStatsFormData()
  } else {
    // 否则就按照传过来的数据去请求数据
    statsFormData.startDate = dateList?.[0] + ' 00:00:00'
    statsFormData.endDate = dateList?.[1] + ' 23:59:59'
  }
  let res = null
  let tableRes = null
  if (group) {
    res = await API.group.queryGroupStats({ ...statsFormData, fullShortUrl, gid })
    tableRes = await API.group.queryGroupTable({ gid, ...statsFormData })
  } else {
    res = await API.smallLinkPage.queryLinkStats({ ...statsFormData, fullShortUrl, gid, enableStatus })
    tableRes = await API.smallLinkPage.queryLinkTable({ gid, fullShortUrl, ...statsFormData, enableStatus })
  }
  tableInfo.value = tableRes
  chartsInfo.value = res?.data?.data
  // debugger
}
// 图表修改时间后重新请求数
const changeTimeData = async (rowInfo, dateList) => {
  const { fullShortUrl, gid, enableStatus } = rowInfo
  if (!dateList) {
    initStatsFormData()
  } else {
    // 否则就按照传过来的数据去请求数据
    statsFormData.startDate = dateList?.[0] + ' 00:00:00'
    statsFormData.endDate = dateList?.[1] + ' 23:59:59'
  }
  let res = null
  let tableRes = null
  // 判断是分组还是单个短链接
  if (isGroup.value) {
    res = await API.group.queryGroupStats({ ...statsFormData, fullShortUrl, gid })
    tableRes = await API.group.queryGroupTable({ gid, ...statsFormData })
  } else {
    res = await API.smallLinkPage.queryLinkStats({ ...statsFormData, fullShortUrl, gid, enableStatus })
    tableRes = await API.smallLinkPage.queryLinkTable({ gid, fullShortUrl, ...statsFormData, enableStatus })
  }
  tableInfo.value = tableRes
  chartsInfo.value = res?.data?.data
}
// 修改时间
const changeTime = (dateList) => {
  changeTimeData(visitLink, dateList)
}
// 修改页码信息
const changePage = async (page) => {
  const { current, size } = page
  statsFormData.current = current ?? 1
  statsFormData.size = size ?? 10
  let tableRes = null
  // 判断是分组还是单个短链接
  if (isGroup.value) {
    tableRes = await API.group.queryGroupTable({ gid: tableGid.value, ...statsFormData })
  } else {
    tableRes = await API.smallLinkPage.queryLinkTable({
      gid: tableGid.value,
      fullShortUrl: tableFullShortUrl.value,
      ...statsFormData
    })
  }
  tableInfo.value = tableRes
}
// 将原来的数据转化为拖拽后传给后端的数据格式
const transformGroupData = (oldIndex, newIndex) => {
  // 相当于直接对展示数据进行修改
  const formData = editableTabs.value
  const tempData = formData.splice(oldIndex, 1)
  // console.log('调整的值', tempData)
  formData.splice(newIndex, 0, tempData[0])
  // console.log('这是经过转化的1', formData)
  formData.forEach((item, index) => {
    item.sortOrder = index
  })
  // console.log('这是经过转化的2', formData)
  return formData
}
// 拖拽
const initSortable = (className) => {
  const table = document.querySelector('.' + className)
  // console.log(table)
  // 创建拖拽实例
  Sortable.create(table, {
    animation: 150, //动画
    // 开始拖动事件
    onStart: () => {
      // console.log('开始拖动')
    },
    // 结束拖动事件
    onEnd: async ({ to, from, oldIndex, newIndex, clone, pullMode }) => {
      // 当oldIndex不等于newIndex时才会去请求接口
      if (newIndex !== oldIndex) {
        // 对于不同情况下数据变化后的选中数据的实现
        if (selectedIndex.value === oldIndex) {
          selectedIndex.value = newIndex
        } else if (
          oldIndex < newIndex &&
          selectedIndex.value > oldIndex &&
          selectedIndex.value <= newIndex
        ) {
          selectedIndex.value = selectedIndex.value - 1
        } else if (
          oldIndex > newIndex &&
          selectedIndex.value < oldIndex &&
          selectedIndex.value >= newIndex
        ) {
          selectedIndex.value = selectedIndex.value + 1
        }
        const res = await API.group.sortGroup(transformGroupData(oldIndex, newIndex))
        // console.log('排序后的结果', res)
      }
    }
  })
}
// 改变选中分组时触发
watch(
  () => selectedIndex.value,
  (newValue) => {
    // -1为回收站，不需要重新请求正常页面数据
    if (newValue !== -1 && newValue !== -2) {
      queryPage()
    }
  }
)
onMounted(() => {
  initSortable('sortOptions')
})
const tableData = ref([])
const pageParams = reactive({
  gid: null,
  current: 1,
  size: 15,
  orderTag: null
})
watch(
  () => pageParams.orderTag,
  (nV) => {
    queryPage()
  }
)
const totalNums = ref(0)
// 当前选中分组名（页面标题）
const currentGroupName = computed(() => editableTabs.value?.[selectedIndex.value]?.name || '投放链接')
// 分组色点：按序号循环取色，便于区分不同投放计划
const groupPalette = ['#6366f1', '#14b8a6', '#f59e0b', '#ec4899', '#0ea5e9', '#8b5cf6', '#22c55e', '#ef4444']
const groupColor = (index) => groupPalette[index % groupPalette.length]
// 概览卡片：对当前页数据求和
const pageSum = (key) => (tableData.value || []).reduce((sum, row) => sum + (Number(row?.[key]) || 0), 0)
// 数据变化后更新当前页面
const queryPage = async () => {
  pageParams.gid = editableTabs.value?.[selectedIndex.value]?.gid
  nums.value = editableTabs.value?.[selectedIndex.value]?.shortLinkCount || 0
  const res = await API.smallLinkPage.queryPage(pageParams)
  if (res?.data.success) {
    tableData.value = res.data?.data?.records
    totalNums.value = +res.data?.data?.total
  } else {
    ElMessage.error(res?.data.message)
  }
}

const handleSizeChange = () => {
  !isRecycleBin.value ? queryPage() : queryRecycleBinPage()
}

const handleCurrentChange = () => {
  !isRecycleBin.value ? queryPage() : queryRecycleBinPage()
}

// 获取分组信息，更新页面的分组模块
const getGroupInfo = async (fn) => {
  const res = await API.group.queryGroup()
  editableTabs.value = res.data?.data?.reverse()
  fn && fn()
}
getGroupInfo(queryPage)

const updatePage = () => {
  getGroupInfo(queryPage)
}

// 是否展示回收站相关的组件
const isRecycleBin = ref(false)
const recycleBinNums = ref(0) // 回收站中的数量
// 获取回收站页面，gid到时候要删除
const queryRecycleBinPage = () => {
  API.smallLinkPage
    .queryRecycleBin({ current: pageParams.current, size: pageParams.size })
    .then((res) => {
      tableData.value = res.data?.data?.records
      totalNums.value = +res.data?.data?.total
      recycleBinNums.value = totalNums.value
    })
}
// 点击回收站
const recycleBin = () => {
  isRecycleBin.value = true
  selectedIndex.value = -1 // -1作为回收站，-2作为选中其他
  pageParams.current = 1
  pageParams.size = 15
  // 点击回收站相关的请求
  queryRecycleBinPage()
}
// 点击分组中的选项
const changeSelectIndex = (index) => {
  selectedIndex.value = index
  isRecycleBin.value = false
  // 对应分组的数据请求
}
// 添加分组相关
const isAddGroup = ref(false)
const newGroupName = ref()
const addGroupLoading = ref(false)
// 展示添加分组弹框
const showAddGroup = () => {
  newGroupName.value = ''
  isAddGroup.value = true
}
// 添加分组
const addGroup = async () => {
  addGroupLoading.value = true
  const res1 = await API.group.addGroup({ name: newGroupName.value })
  if (res1?.data.success) {
    ElMessage.success('添加成功')
    getGroupInfo(queryPage)
  } else {
    ElMessage.error(res1?.data.message)
  }
  isAddGroup.value = false
  addGroupLoading.value = false
}
// 删除分组
const deleteGroup = async (gid) => {
  const res = await API.group.deleteGroup({ gid })
  selectedIndex.value = 0
  if (res.data.success) {
    ElMessage.success('删除成功')
    getGroupInfo(queryPage)
  } else {
    ElMessage.error(res.data.message)
  }
}
// 编辑分组
const isEditGroup = ref(false)
const editGroupName = ref()
const editGid = ref('')
const editGroupLoading = ref(false)
// 点击编辑按钮，出现编辑弹框
const showEditGroup = (gid, name) => {
  editGid.value = gid
  editGroupName.value = name
  isEditGroup.value = true
}
// 编辑分组标题
const editGroup = async () => {
  editGroupLoading.value = true
  const res = await API.group.editGroup({ gid: editGid.value, name: editGroupName.value })
  if (res.data.success) {
    ElMessage.success('编辑成功')
    getGroupInfo(queryPage)
  } else {
    ElMessage.error('编辑失败')
  }
  isEditGroup.value = false
  editGroupLoading.value = false
}
// 创建短链
const isAddSmallLink = ref(false)
const isAddSmallLinks = ref(false)
// 关闭新建短链接弹窗
const addLink = () => {
  isAddSmallLink.value = false
  isAddSmallLinks.value = false
}
// 新建批量新建短链接弹窗
const cancelAddLink = () => {
  isAddSmallLink.value = false
  isAddSmallLinks.value = false
}
const getImgUrl = (url) => {
  return url ?? defaultImg
}
// 判断链接是否过期
const isExpire = (validDate) => {
  if (validDate) {
    const date = new Date(validDate).getTime()
    return new Date().getTime() < date
  }
}
// 复制链接
const copyUrl = (url) => {
  let eInput = document.createElement('input')
  eInput.value = url
  document.body.appendChild(eInput)
  eInput.select()
  let copyText = document.execCommand('Copy')
  eInput.style.display = 'none'
  if (copyText) {
    // console.log(eInput.value)
    ElMessage.success('链接复制成功!')
  }
}
// 修改短链信息
const isEditLink = ref(false) // 是否展示编辑弹框
const editLinkRef = ref()
const editData = ref() // 编辑弹框的数据
const editLink = (data) => {
  // console.log('data: ---' + data)
  editData.value = data
  isEditLink.value = true
}
// 隐藏弹框
const coverEditLink = () => {
  isEditLink.value = false
}
// 移动到回收站
const toRecycleBin = (data) => {
  const { gid, fullShortUrl } = data
  API.smallLinkPage
    .toRecycleBin({ gid, fullShortUrl })
    .then((res) => {
      if (res?.data?.code !== '0') {
        ElMessage.error(res.data.message)
      } else {
        ElMessage.success('删除成功')
        getGroupInfo(queryPage)
      }
    })
    .catch((reason) => {
      ElMessage.error('删除失败')
    })
}
// 回收站中恢复
const recoverLink = (data) => {
  const { gid, fullShortUrl } = data
  API.smallLinkPage
    .recoverLink({ gid, fullShortUrl })
    .then((res) => {
      ElMessage.success('恢复成功')
      queryRecycleBinPage()
      // getGroupInfo(queryPage)
      getGroupInfo()         //修复短链接恢复会报系统执行出错的问题
    })
    .catch((reason) => {
      ElMessage.error('恢复失败')
    })
}
// 从回收站中删除
const removeLink = (data) => {
  const { gid, fullShortUrl } = data
  API.smallLinkPage
    .removeLink({ gid, fullShortUrl })
    .then((res) => {
      if (res?.data?.code !== '0') {
        ElMessage.error(res.data.message)
      } else {
        ElMessage.success('删除成功')
        queryRecycleBinPage()
      }
    })
    .catch((reason) => {
      ElMessage.error('删除失败')
    })
}
</script>

<style lang="scss" scoped>
.workspace {
  display: flex;
  height: 100%;
}

/* ---------------- 分组侧栏 ---------------- */
.group-panel {
  position: relative;
  flex: none;
  display: flex;
  flex-direction: column;
  width: 248px;
  height: 100%;
  background: var(--sl-surface);
  border-right: 1px solid var(--sl-border);
}

.group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 16px 14px 20px;
}

.group-head-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--sl-text);
}

.group-head-sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--sl-text-3);
}

.sortOptions {
  flex: 1;
  min-height: 0;
  padding: 0 10px 8px;
  overflow-y: auto;
  overflow-x: hidden;
}

.group-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  margin-bottom: 2px;
  padding: 0 10px 0 6px;
  border-radius: 8px;
  color: var(--sl-text-2);
  cursor: pointer;
  user-select: none;
  transition: background 0.15s, color 0.15s;

  &:hover {
    color: var(--sl-text);
    background: #f4f5fa;

    .drag-handle {
      opacity: 1;
    }

    .group-actions {
      display: flex;
    }

    .item-length {
      display: none;
    }
  }
}

.selectedItem,
.selectedItem:hover {
  color: var(--sl-primary) !important;
  background: var(--sl-primary-soft) !important;
  font-weight: 600;

  .item-length {
    color: var(--sl-primary);
    background: #fff;
  }
}

.drag-handle {
  flex: none;
  font-size: 13px;
  color: var(--sl-text-3);
  opacity: 0;
  cursor: grab;
  transition: opacity 0.15s;
}

.group-dot {
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 3px;
}

.group-name {
  flex: 1;
  min-width: 0;
}

.group-actions {
  display: none;
  align-items: center;
  gap: 2px;
}

.edit {
  padding: 4px;
  border-radius: 6px;
  font-size: 16px;
  color: var(--sl-text-3);
  box-sizing: content-box;

  &:hover {
    color: var(--sl-primary);
    background: #fff;
  }
}

.item-length {
  flex: none;
  min-width: 24px;
  padding: 0 6px;
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
  text-align: center;
  border-radius: 999px;
  color: var(--sl-text-3);
  background: #f1f3f8;
}

.recycle-bin {
  flex: none;
  padding: 10px;
  border-top: 1px solid var(--sl-border-soft);
}

.recycle-box {
  margin: 0;
  padding-left: 12px;
}

.recycle-icon {
  font-size: 16px;
}

/* ---------------- 主内容 ---------------- */
.content-box {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 24px 28px 20px;
  overflow: hidden;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  line-height: 1.3;
  font-weight: 700;
  color: var(--sl-text);
}

.page-sub {
  margin-top: 4px;
  font-size: 13px;
  color: var(--sl-text-3);
}

.page-actions {
  display: flex;
  flex: none;

  .el-button {
    height: 38px;
    padding: 0 18px;
    border-radius: 9px;
  }
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border: 1px solid var(--sl-border);
  border-radius: var(--sl-radius);
  background: var(--sl-surface);
  box-shadow: var(--sl-shadow);
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.07);
  }
}

.stat-icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 42px;
  height: 42px;
  border-radius: 11px;
  font-size: 20px;

  &.indigo {
    color: #4f46e5;
    background: #eef0ff;
  }

  &.teal {
    color: #0d9488;
    background: #e6faf7;
  }

  &.amber {
    color: #d97706;
    background: #fff6e5;
  }

  &.violet {
    color: #7c3aed;
    background: #f4eeff;
  }
}

.stat-label {
  font-size: 12px;
  color: var(--sl-text-3);

  .hint {
    margin-left: 2px;
    padding: 0 5px;
    font-size: 10px;
    border-radius: 4px;
    background: #f1f3f8;
  }
}

.stat-value {
  margin-top: 2px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--sl-text);
  font-variant-numeric: tabular-nums;
}

.table-box {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--sl-border);
  border-radius: var(--sl-radius);
  background: var(--sl-surface);
  box-shadow: var(--sl-shadow);
  overflow: hidden;
}

.link-table {
  flex: 1;
  min-height: 0;

  :deep(.el-table__cell) {
    padding: 12px 0;
  }
}

.pagination-block {
  flex: none;
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
  border-top: 1px solid var(--sl-border-soft);
}

.sort-head {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 2px;
  color: #64748b;
  font-weight: 500;
  cursor: pointer;

  &:hover {
    color: var(--sl-primary);
  }
}

.orderIndex {
  color: var(--sl-primary);
}

/* 单元格 */
.table-link-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.favicon {
  display: grid;
  place-items: center;
  flex: none;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--sl-border-soft);
  background: #f8fafc;
}

.name-date {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.link-title,
.origin-url {
  display: -webkit-box;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.link-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--sl-text);
}

.time {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--sl-text-3);
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 500;
  line-height: 18px;
  border-radius: 5px;
}

.badge-warn {
  color: #b45309;
  background: #fff6e5;
}

.badge-danger {
  color: #dc2626;
  background: #fef2f2;
}

.isExpire {
  .favicon {
    opacity: 0.5;
  }

  .link-title {
    color: var(--sl-text-3);
    text-decoration: line-through;
  }
}

.table-url-box {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
}

.short-url {
  font-family: var(--sl-mono);
  font-size: 13px;
  font-weight: 500;
}

.origin-url {
  font-size: 12px;
  color: var(--sl-text-3);
}

.metric {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
  font-variant-numeric: tabular-nums;
}

.metric-today {
  font-size: 15px;
  font-weight: 600;
  color: var(--sl-text);

  em {
    margin-left: 4px;
    font-size: 11px;
    font-style: normal;
    font-weight: 400;
    color: var(--sl-text-3);
  }
}

.metric-total {
  font-size: 12px;
  color: var(--sl-text-3);
}

.quick-actions,
.row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 通用图标按钮 */
.icon-btn {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  font-size: 16px;
  color: var(--sl-text-2);
  background: transparent;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    color: var(--sl-primary);
    background: var(--sl-primary-soft);
  }

  &.danger:hover {
    color: var(--sl-danger);
    background: #fef2f2;
  }

  &.primary {
    color: var(--sl-primary);
    background: var(--sl-primary-soft);

    &:hover {
      color: #fff;
      background: var(--sl-primary);
    }
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 72px 0;
  line-height: 1.6;
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  border-radius: 18px;
  font-size: 28px;
  color: var(--sl-primary);
  background: var(--sl-primary-soft);
}

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--sl-text);
}

.empty-sub {
  margin: 2px 0 18px;
  font-size: 13px;
  color: var(--sl-text-3);
}

.over-text {
  display: -webkit-box;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

/* 弹框中的 tabs */
.custom-tabs-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;

  .soon {
    margin-left: 4px;
    padding: 0 6px;
    font-size: 10px;
    line-height: 16px;
    border-radius: 4px;
    color: var(--sl-text-3);
    background: #f1f3f8;
  }
}

@media (max-width: 1280px) {
  .stat-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

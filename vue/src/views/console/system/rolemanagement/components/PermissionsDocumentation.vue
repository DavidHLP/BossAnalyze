<template>
  <el-dialog
    :model-value="dialogVisible"
    @update:model-value="updateDialog"
    width="900px"
    destroy-on-close
    top="3vh"
    class="permissions-documentation-dialog"
    :show-close="false"
  >
    <template #header>
      <div class="dialog-header">
        <div class="header-content">
          <div class="header-icon">
            <el-icon><InfoFilled /></el-icon>
          </div>
          <div class="header-text">
            <h2 class="dialog-title">权限管理说明文档</h2>
            <p class="dialog-subtitle">系统权限体系详细说明与使用指南</p>
          </div>
        </div>
        <el-button
          type="primary"
          text
          circle
          @click="updateDialog(false)"
          class="close-btn"
        >
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </template>

    <div class="documentation-content">
      <!-- 导航标签 -->
      <el-tabs v-model="activeTab" class="documentation-tabs">
        <!-- 权限概述 -->
        <el-tab-pane label="权限概述" name="overview">
          <div class="tab-content">
            <div class="overview-grid">
              <div class="feature-card">
                <div class="feature-icon menu">
                  <el-icon><Menu /></el-icon>
                </div>
                <h3 class="feature-title">菜单权限</h3>
                <p class="feature-desc">控制用户可访问的菜单页面，基于路由配置自动生成权限树</p>
                <ul class="feature-list">
                  <li>页面访问控制</li>
                  <li>路由级别权限</li>
                  <li>层级权限继承</li>
                </ul>
              </div>

              <div class="feature-card">
                <div class="feature-icon operation">
                  <el-icon><Setting /></el-icon>
                </div>
                <h3 class="feature-title">操作权限</h3>
                <p class="feature-desc">细粒度的功能操作控制，包括增删改查等具体操作</p>
                <ul class="feature-list">
                  <li>按钮级别控制</li>
                  <li>API 接口权限</li>
                  <li>数据操作权限</li>
                </ul>
              </div>

              <div class="feature-card">
                <div class="feature-icon data">
                  <el-icon><DataBoard /></el-icon>
                </div>
                <h3 class="feature-title">数据权限</h3>
                <p class="feature-desc">基于数据范围的访问控制，支持多维度数据隔离</p>
                <ul class="feature-list">
                  <li>部门数据隔离</li>
                  <li>个人数据权限</li>
                  <li>自定义数据范围</li>
                </ul>
              </div>
            </div>

            <div class="permission-flow">
              <h3 class="section-title">权限验证流程</h3>
              <div class="flow-steps">
                <div class="step">
                  <div class="step-number">1</div>
                  <div class="step-content">
                    <h4>用户登录</h4>
                    <p>系统验证用户身份</p>
                  </div>
                </div>
                <div class="step-arrow">→</div>
                <div class="step">
                  <div class="step-number">2</div>
                  <div class="step-content">
                    <h4>角色识别</h4>
                    <p>获取用户关联角色</p>
                  </div>
                </div>
                <div class="step-arrow">→</div>
                <div class="step">
                  <div class="step-number">3</div>
                  <div class="step-content">
                    <h4>权限加载</h4>
                    <p>加载角色对应权限</p>
                  </div>
                </div>
                <div class="step-arrow">→</div>
                <div class="step">
                  <div class="step-number">4</div>
                  <div class="step-content">
                    <h4>访问控制</h4>
                    <p>验证操作权限</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 角色管理 -->
        <el-tab-pane label="角色管理" name="roles">
          <div class="tab-content">
            <div class="roles-section">
              <h3 class="section-title">系统内置角色</h3>
              <div class="roles-table-container">
                <el-table :data="builtInRoles" class="roles-table">
                  <el-table-column width="80">
                    <template #default="{ row }">
                      <div class="role-icon" :class="row.type">
                        <el-icon v-if="row.type === 'admin'"><Lock /></el-icon>
                        <el-icon v-else-if="row.type === 'user'"><User /></el-icon>
                        <el-icon v-else><UserFilled /></el-icon>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="name" label="角色名称" width="120">
                    <template #default="{ row }">
                      <span class="role-name">{{ row.name }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="description" label="角色描述" min-width="200" />
                  <el-table-column prop="permissions" label="权限范围" width="150">
                    <template #default="{ row }">
                      <el-tag :type="row.permissionType" size="small">
                        {{ row.permissions }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="editable" label="可编辑性" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag
                        size="small"
                        :type="row.editable ? 'success' : 'danger'"
                        class="editable-tag"
                      >
                        {{ row.editable ? '可编辑' : '系统锁定' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <div class="role-tips">
                <el-alert
                  type="warning"
                  :closable="false"
                  class="tip-alert"
                >
                  <template #title>
                    <el-icon><WarningFilled /></el-icon>
                    重要提示
                  </template>
                  <ul class="tip-list">
                    <li><strong>ADMIN 角色</strong>：系统超级管理员，拥有所有权限，不可删除或修改权限</li>
                    <li><strong>USER 角色</strong>：系统默认角色，新用户默认分配，可修改权限但不可删除</li>
                    <li><strong>自定义角色</strong>：可根据业务需求创建，支持灵活的权限配置</li>
                  </ul>
                </el-alert>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 权限配置 -->
        <el-tab-pane label="权限配置" name="configuration">
          <div class="tab-content">
            <div class="config-section">
              <h3 class="section-title">权限配置指南</h3>

              <div class="config-steps">
                <div class="config-step">
                  <div class="step-header">
                    <div class="step-icon create">
                      <el-icon><Plus /></el-icon>
                    </div>
                    <h4>创建角色</h4>
                  </div>
                  <div class="step-content">
                    <p>点击"新建角色"按钮，填写角色基本信息：</p>
                    <ul>
                      <li>角色名称：建议使用语义化命名</li>
                      <li>角色描述：详细说明角色用途</li>
                      <li>状态设置：启用或禁用</li>
                    </ul>
                  </div>
                </div>

                <div class="config-step">
                  <div class="step-header">
                    <div class="step-icon permission">
                      <el-icon><Setting /></el-icon>
                    </div>
                    <h4>分配权限</h4>
                  </div>
                  <div class="step-content">
                    <p>在权限设置页面选择菜单权限：</p>
                    <ul>
                      <li>勾选菜单项自动获得访问权限</li>
                      <li>父级菜单包含子级菜单权限</li>
                      <li>支持批量选择和取消</li>
                    </ul>
                  </div>
                </div>

                <div class="config-step">
                  <div class="step-header">
                    <div class="step-icon assign">
                      <el-icon><UserFilled /></el-icon>
                    </div>
                    <h4>分配用户</h4>
                  </div>
                  <div class="step-content">
                    <p>在用户管理中为用户分配角色：</p>
                    <ul>
                      <li>每个用户可分配多个角色</li>
                      <li>权限采用并集模式</li>
                      <li>立即生效，无需重新登录</li>
                    </ul>
                  </div>
                </div>
              </div>

              <div class="best-practices">
                <h4 class="practices-title">
                  <el-icon><StarFilled /></el-icon>
                  最佳实践建议
                </h4>
                <div class="practices-grid">
                  <div class="practice-card">
                    <h5>最小权限原则</h5>
                    <p>为角色分配完成工作所需的最小权限集合，避免权限过度分配</p>
                  </div>
                  <div class="practice-card">
                    <h5>角色职责分离</h5>
                    <p>根据岗位职责创建不同角色，避免一个角色承担过多功能</p>
                  </div>
                  <div class="practice-card">
                    <h5>定期权限审计</h5>
                    <p>定期审查角色权限配置，及时回收不必要的权限</p>
                  </div>
                  <div class="practice-card">
                    <h5>权限变更记录</h5>
                    <p>记录权限变更历史，便于追溯和安全审计</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 常见问题 -->
        <el-tab-pane label="常见问题" name="faq">
          <div class="tab-content">
            <div class="faq-section">
              <h3 class="section-title">常见问题解答</h3>

              <el-collapse v-model="activeFaq" class="faq-collapse">
                <el-collapse-item
                  v-for="(faq, index) in faqList"
                  :key="index"
                  :title="faq.question"
                  :name="index"
                  class="faq-item"
                >
                  <div class="faq-answer" v-html="faq.answer"></div>
                </el-collapse-item>
              </el-collapse>

              <div class="contact-section">
                <el-alert
                  type="info"
                  :closable="false"
                  class="contact-alert"
                >
                  <template #title>
                    <el-icon><QuestionFilled /></el-icon>
                    需要帮助？
                  </template>
                  <p>如果您在使用过程中遇到其他问题，请联系系统管理员或查看详细的用户手册。</p>
                  <div class="contact-methods">
                    <el-button type="primary" size="small">
                      <el-icon><ChatDotSquare /></el-icon>
                      在线客服
                    </el-button>
                    <el-button type="success" size="small">
                      <el-icon><Document /></el-icon>
                      用户手册
                    </el-button>
                  </div>
                </el-alert>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import {
  InfoFilled,
  Close,
  Menu,
  Setting,
  DataBoard,
  Lock,
  User,
  UserFilled,
  WarningFilled,
  Plus,
  StarFilled,
  QuestionFilled,
  ChatDotSquare,
  Document
} from '@element-plus/icons-vue';

defineProps<{
  dialogVisible: boolean
}>();

const emit = defineEmits(['update:dialogVisible']);

// 更新对话框状态
const updateDialog = (value: boolean) => {
  emit('update:dialogVisible', value);
};

// 当前活跃的标签页
const activeTab = ref('overview');

// 当前展开的FAQ
const activeFaq = ref<number[]>([0]);

// 内置角色数据
const builtInRoles = ref([
  {
    name: 'ADMIN',
    description: '系统超级管理员，拥有所有系统功能的完整访问权限',
    permissions: '完全权限',
    permissionType: 'danger',
    editable: false,
    type: 'admin'
  },
  {
    name: 'USER',
    description: '系统默认角色，拥有基础的浏览和个人信息管理权限',
    permissions: '基础权限',
    permissionType: 'primary',
    editable: true,
    type: 'user'
  }
]);

// FAQ数据
const faqList = ref([
  {
    question: '为什么分配了菜单权限后用户还是看不到相关功能？',
    answer: `
      <p>可能的原因包括：</p>
      <ul>
        <li><strong>用户未重新登录</strong>：权限更改后建议用户重新登录以刷新权限缓存</li>
        <li><strong>父级菜单未授权</strong>：子菜单需要父级菜单权限才能正常显示</li>
        <li><strong>角色状态异常</strong>：检查角色是否处于启用状态</li>
        <li><strong>权限缓存问题</strong>：联系管理员清理权限缓存</li>
      </ul>
    `
  },
  {
    question: '如何理解菜单权限的层级关系？',
    answer: `
      <p>菜单权限采用树形结构：</p>
      <ul>
        <li><strong>父子关系</strong>：访问子菜单必须拥有父菜单权限</li>
        <li><strong>继承机制</strong>：勾选父菜单会自动勾选所有子菜单</li>
        <li><strong>独立配置</strong>：可以单独取消某个子菜单权限</li>
        <li><strong>最小权限</strong>：系统会自动计算最小必需权限集合</li>
      </ul>
    `
  },
  {
    question: '一个用户可以拥有多个角色吗？',
    answer: `
      <p>是的，系统支持多角色分配：</p>
      <ul>
        <li><strong>权限合并</strong>：多个角色的权限会自动合并（并集）</li>
        <li><strong>优先级</strong>：不存在角色优先级，所有权限平等</li>
        <li><strong>管理便利</strong>：可以通过组合不同角色实现灵活的权限配置</li>
        <li><strong>审计追踪</strong>：系统会记录每个角色的权限来源</li>
      </ul>
    `
  },
  {
    question: '系统角色和自定义角色有什么区别？',
    answer: `
      <p>两种角色类型的主要区别：</p>
      <table style="width: 100%; border-collapse: collapse; margin-top: 10px;">
        <tr style="background: #f5f7fa;">
          <th style="padding: 8px; border: 1px solid #ddd;">特性</th>
          <th style="padding: 8px; border: 1px solid #ddd;">系统角色</th>
          <th style="padding: 8px; border: 1px solid #ddd;">自定义角色</th>
        </tr>
        <tr>
          <td style="padding: 8px; border: 1px solid #ddd;"><strong>可删除性</strong></td>
          <td style="padding: 8px; border: 1px solid #ddd;">不可删除</td>
          <td style="padding: 8px; border: 1px solid #ddd;">可删除</td>
        </tr>
        <tr>
          <td style="padding: 8px; border: 1px solid #ddd;"><strong>权限修改</strong></td>
          <td style="padding: 8px; border: 1px solid #ddd;">ADMIN不可修改，USER可修改</td>
          <td style="padding: 8px; border: 1px solid #ddd;">完全可修改</td>
        </tr>
        <tr>
          <td style="padding: 8px; border: 1px solid #ddd;"><strong>用途</strong></td>
          <td style="padding: 8px; border: 1px solid #ddd;">系统基础功能</td>
          <td style="padding: 8px; border: 1px solid #ddd;">业务定制需求</td>
        </tr>
      </table>
    `
  },
  {
    question: '如何确保权限配置的安全性？',
    answer: `
      <p>建议遵循以下安全原则：</p>
      <ul>
        <li><strong>最小权限原则</strong>：只分配完成工作必需的权限</li>
        <li><strong>定期审查</strong>：定期检查和清理不必要的权限</li>
        <li><strong>职责分离</strong>：避免单一角色拥有过多权限</li>
        <li><strong>变更记录</strong>：记录所有权限变更的时间和操作人</li>
        <li><strong>测试验证</strong>：权限配置后进行充分测试</li>
      </ul>
    `
  }
]);
</script>

<style scoped lang="scss">
@use '../rolemanagement.scss' as *;
</style>

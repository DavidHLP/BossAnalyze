<template>
  <div class="base-tree-container">
    <el-tree
      ref="treeRef"
      :data="treeData"
      show-checkbox
      node-key="id"
      :props="defaultProps"
      :default-checked-keys="defaultCheckedKeys"
      :check-strictly="checkStrictly"
      :default-expand-all="defaultExpandAll"
      :expand-on-click-node="expandOnClickNode"
      highlight-current
      @check-change="handleCheckChange"
      class="custom-tree"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue';

interface TreeNode {
  id: number;
  name: string;
  children?: TreeNode[];
  parent?: TreeNode;
  level?: number;
}

interface Props {
  treeData: TreeNode[];
  defaultCheckedKeys?: number[];
  checkStrictly?: boolean;
  defaultExpandAll?: boolean;
  expandOnClickNode?: boolean;
  defaultProps?: {
    label: string;
    children: string;
  };
}

const props = withDefaults(defineProps<Props>(), {
  defaultCheckedKeys: () => [],
  checkStrictly: true,
  defaultExpandAll: false,
  expandOnClickNode: false,
  defaultProps: () => ({
    label: 'name',
    children: 'children'
  })
});

const emit = defineEmits<{
  (e: 'update:checkedKeys', keys: number[]): void;
  (e: 'check-change', data: TreeNode, checked: boolean): void;
}>();

const treeRef = ref();

// 初始化树形结构
const initTree = () => {
  if (!treeRef.value) return;

  treeRef.value.setCheckedKeys([]);
  treeRef.value.setCheckedKeys(props.defaultCheckedKeys);

  // 确保所有选中节点的父节点也被选中
  nextTick(() => {
    // 获取树的所有节点数据
    const allNodes = getAllTreeNodes(props.treeData);

    // 找到所有已选中的节点
    const checkedNodes = allNodes.filter(node =>
      props.defaultCheckedKeys.includes(node.id)
    );

    // 为每个选中的节点处理其父节点选中状态
    checkedNodes.forEach(node => {
      // 找到节点在树中的完整引用（包含parent属性）
      const treeNode = treeRef.value.getNode(node.id);
      if (treeNode) {
        checkParentNodes(treeNode);
      }
    });

    // 确保已选中节点的视图更新
    nextTick(() => {
      ensureParentNodesChecked();
    });
  });
};

// 确保所有已选中节点的父节点都被选中
const ensureParentNodesChecked = () => {
  if (!treeRef.value) return;

  // 获取当前所有选中的节点
  const checkedNodes = treeRef.value.getCheckedNodes();

  // 对每个选中的节点，确保其父节点也被选中
  checkedNodes.forEach((checkedNode: TreeNode) => {
    // 从树中获取完整的节点引用，包含父节点信息
    const treeNode = treeRef.value.getNode(checkedNode.id);
    if (treeNode && treeNode.parent) {
      // 递归勾选父节点
      checkParentNodes(treeNode);
    }
  });
};

// 递归获取树的所有节点
const getAllTreeNodes = (nodes: TreeNode[]): TreeNode[] => {
  let allNodes: TreeNode[] = [];

  nodes.forEach(node => {
    allNodes.push(node);
    if (node.children && node.children.length) {
      allNodes = allNodes.concat(getAllTreeNodes(node.children));
    }
  });

  return allNodes;
};

// 递归勾选所有父节点
const checkParentNodes = (node: TreeNode) => {
  if (!treeRef.value) return;

  // 如果节点不存在、没有父节点或父节点没有ID，则返回
  if (!node || !node.parent || node.parent.id === undefined) return;

  // 如果是根节点或父节点没有ID，则返回
  if (node.parent.level === 0) return;

  // 检查父节点是否已勾选，避免不必要的操作
  if (!treeRef.value.getCheckedKeys().includes(node.parent.id)) {
    // 勾选父节点，但不级联子节点
    treeRef.value.setChecked(node.parent.id, true, false);
  }

  // 递归处理更上层的父节点
  checkParentNodes(node.parent);
};

// 处理节点勾选状态变化事件
const handleCheckChange = (data: TreeNode, checked: boolean) => {
  if (checked) {
    // 当节点被勾选时，获取节点的完整引用
    const currentNode = treeRef.value.getNode(data.id);

    // 如果节点存在并且有父节点，勾选所有父节点
    if (currentNode && currentNode.parent) {
      checkParentNodes(currentNode);
    }

    // 使用nextTick确保DOM更新后，状态完全同步
    nextTick(() => {
      ensureParentNodesChecked();
    });
  }

  // 获取当前所有选中的节点ID
  const checkedKeys = treeRef.value.getCheckedKeys();
  emit('update:checkedKeys', checkedKeys);
  emit('check-change', data, checked);
};

// 重置树形结构
const resetTree = () => {
  if (!treeRef.value) return;
  treeRef.value.setCheckedKeys([]);
};

// 设置选中的节点
const setCheckedKeys = (keys: number[]) => {
  if (!treeRef.value) return;
  treeRef.value.setCheckedKeys(keys);
};

// 获取选中的节点
const getCheckedKeys = () => {
  if (!treeRef.value) return [];
  return treeRef.value.getCheckedKeys();
};

// 监听默认选中值的变化
watch(() => props.defaultCheckedKeys, () => {
  if (treeRef.value) {
    initTree();
  }
}, { deep: true });

// 暴露方法给父组件
defineExpose({
  resetTree,
  setCheckedKeys,
  getCheckedKeys,
  initTree
});
</script>

<style scoped lang="scss">
.base-tree-container {
  .custom-tree {
    :deep(.el-tree-node__content) {
      height: 36px;

      &:hover {
        background-color: #f0f9ff;
      }
    }

    :deep(.is-current) .el-tree-node__content {
      background-color: #E0F2FE;
    }
  }
}
</style>

import { ref, reactive, computed } from 'vue'
import type { NodeData, EdgeData } from '@antv/g6'

interface MyGraphData {
  nodes: NodeData[]
  edges: EdgeData[]
}

export function useGit() {
  // -- State Management --
  const initialCommit: NodeData = {
    id: 'commit-0',
    data: { label: 'Initial commit', branch: 'main' },
  }

  const contentHistory = reactive<Record<string, string>>({
    [initialCommit.id as string]: '# Welcome to the Git-like Editor!',
  })
  const graphData = reactive<MyGraphData>({
    nodes: [initialCommit],
    edges: [],
  })
  const branches = reactive<Record<string, string>>({
    main: initialCommit.id as string,
  })
  const stashStack = reactive<{ message: string; content: string; branch: string; head: string }[]>([])

  const currentBranch = ref('main')
  const currentContent = ref(contentHistory[initialCommit.id as string])
  const head = ref(initialCommit.id as string)
  let commitCounter = 1

  // -- Computed Status --
  const isDetachedHead = computed(() => !branches[currentBranch.value] || branches[currentBranch.value] !== head.value)
  const statusMessage = computed(() => {
    if (isDetachedHead.value) {
      return `HEAD is detached at ${head.value.substring(0, 7)}.`
    }
    return `On branch ${currentBranch.value}`
  })

  // -- Helpers --
  const getCommit = (commitId: string) => graphData.nodes.find(n => n.id === commitId);
  const getCommitContent = (commitId: string) => contentHistory[commitId];
  const getAncestors = (commitId: string): Set<string> => {
    const ancestors = new Set<string>();
    const queue = [commitId];
    const visited = new Set<string>();

    while (queue.length > 0) {
      const current = queue.shift()!;
      if (visited.has(current)) continue;
      visited.add(current);

      const parents = graphData.edges.filter(e => e.target === current).map(e => e.source!);
      parents.forEach(p => {
        ancestors.add(p);
        queue.push(p);
      });
    }
    return ancestors;
  }

  // -- Actions --
  const commit = (message: string) => {
    if (!message.trim()) return 'Commit message cannot be empty!'
    if (isDetachedHead.value) return "Cannot commit in detached HEAD state. Please create a new branch."

    const newCommitId = `commit-${commitCounter++}`
    const newCommit: NodeData = {
      id: newCommitId,
      data: { label: message, branch: currentBranch.value },
    }

    contentHistory[newCommitId] = currentContent.value
    graphData.nodes.push(newCommit)
    graphData.edges.push({
      source: head.value,
      target: newCommitId,
      data: { branch: currentBranch.value },
    })

    head.value = newCommitId
    branches[currentBranch.value] = newCommitId
    return null
  }

  const createBranch = (name: string) => {
    if (!name.trim()) return 'Branch name cannot be empty!'
    if (branches[name]) return `Branch '${name}' already exists!`

    branches[name] = head.value
    currentBranch.value = name
    return null
  }

  const switchBranch = (name: string) => {
    if (!branches[name]) return `Branch '${name}' not found!`

    currentBranch.value = name
    head.value = branches[name]
    currentContent.value = getCommitContent(head.value)
    return null
  }

  const merge = (sourceBranch: string) => {
    if (!sourceBranch || !branches[sourceBranch]) return 'Source branch not found!'

    const targetBranch = currentBranch.value
    const sourceHead = branches[sourceBranch]
    const targetHead = branches[targetBranch]

    if (sourceHead === targetHead) return 'Already up-to-date.'
    if (getAncestors(targetHead).has(sourceHead)) return 'Already up-to-date. Source branch is an ancestor.';

    const commitMsg = `Merge branch '${sourceBranch}' into ${targetBranch}`
    const newCommitId = `commit-${commitCounter++}`
    const newCommit: NodeData = {
      id: newCommitId,
      data: { label: commitMsg, branch: targetBranch, type: 'merge' },
    }

    contentHistory[newCommitId] = currentContent.value
    graphData.nodes.push(newCommit)
    graphData.edges.push({ source: targetHead, target: newCommitId, data: { branch: targetBranch } })
    graphData.edges.push({ source: sourceHead, target: newCommitId, data: { branch: sourceBranch } })

    head.value = newCommitId
    branches[targetBranch] = newCommitId
    return null
  }

  const checkout = (commitId: string) => {
    if (!contentHistory[commitId]) return 'Commit not found!'
    head.value = commitId
    currentContent.value = getCommitContent(commitId)

    const branchName = Object.keys(branches).find(b => branches[b] === commitId)
    if (branchName) {
      currentBranch.value = branchName
    }
    return null
  }

  const reset = (commitId: string) => {
    if (isDetachedHead.value) return "Cannot reset in detached HEAD state."
    if (!contentHistory[commitId]) return "Commit not found."

    const branchHeadId = branches[currentBranch.value];
    const branchAncestors = getAncestors(branchHeadId);
    if (!branchAncestors.has(commitId) && branchHeadId !== commitId) {
      return "Cannot reset to a commit that is not an ancestor of the current branch head."
    }

    branches[currentBranch.value] = commitId
    checkout(commitId)
    return null
  }

  const stash = () => {
    const hasChanges = currentContent.value !== getCommitContent(head.value)
    if (!hasChanges) return "No local modifications to save."

    const stashMessage = `WIP on ${currentBranch.value}: ${head.value.substring(0, 7)} ${getCommit(head.value)?.data?.label}`
    stashStack.push({
      message: stashMessage,
      content: currentContent.value,
      branch: currentBranch.value,
      head: head.value,
    })
    currentContent.value = getCommitContent(head.value)
    return `Saved working directory and index state "${stashMessage}"`
  }

  const stashPop = () => {
    if (stashStack.length === 0) return "No stash entries found."

    const stashEntry = stashStack.pop()
    if (stashEntry) {
      switchBranch(stashEntry.branch)
      checkout(stashEntry.head)
      currentContent.value = stashEntry.content
      return `Dropped refs/stash@{0} (${stashEntry.message})`
    }
    return null
  }

  const cherryPick = (commitId: string) => {
    if (isDetachedHead.value) return "Cannot cherry-pick in detached HEAD state."

    const commitToPick = getCommit(commitId);
    if (!commitToPick) return "Commit to cherry-pick not found.";

    const newCommitId = `commit-${commitCounter++}`
    const newCommit: NodeData = {
      id: newCommitId,
      data: {
        label: commitToPick.data!.label,
        branch: currentBranch.value,
        type: 'cherry-pick'
      }
    }

    contentHistory[newCommitId] = getCommitContent(commitId);
    graphData.nodes.push(newCommit);
    graphData.edges.push({
      source: head.value,
      target: newCommitId,
      data: { branch: currentBranch.value }
    });
    head.value = newCommitId;
    branches[currentBranch.value] = newCommitId;

    return `Cherry-picked commit ${commitId.substring(0, 7)}.`;
  }

  const rebase = (baseBranch: string) => {
    if (isDetachedHead.value) return "Cannot rebase in detached HEAD state."
    if (!branches[baseBranch]) return `Base branch '${baseBranch}' not found.`

    const headBranch = currentBranch.value;
    if (headBranch === baseBranch) return "Cannot rebase a branch onto itself."

    const baseHead = branches[baseBranch];
    const headHead = branches[headBranch];

    const baseAncestors = getAncestors(baseHead);
    if (baseAncestors.has(headHead)) return `Branch '${headBranch}' is an ancestor of '${baseBranch}'. Rebase is not needed.`

    // Find common ancestor
    const headAncestors = getAncestors(headHead);
    let commonAncestor = initialCommit.id as string;
    for (const A of headAncestors) {
      if (getAncestors(baseHead).has(A) || A === baseHead) {
        commonAncestor = A;
        break;
      }
    }

    // Get commits to rebase
    const commitsToRebase: NodeData[] = [];
    let currentCommitId = headHead;
    while (currentCommitId !== commonAncestor) {
      const commitNode = getCommit(currentCommitId);
      if (!commitNode) break;
      commitsToRebase.unshift(commitNode as NodeData);
      const parentEdge = graphData.edges.find(e => {
        const sourceCommit = getCommit(e.source as string);
        const isMergeCommit = sourceCommit?.data?.type === 'merge';
        return e.target === currentCommitId && !isMergeCommit;
      });
      if (!parentEdge) break;
      currentCommitId = parentEdge.source as string;
    }

    if (commitsToRebase.length === 0) return "Branch is up to date with base branch, nothing to rebase."

    let newHead = baseHead;
    // Apply commits on top of new base
    commitsToRebase.forEach(commitToRebase => {
      const newCommitId = `commit-${commitCounter++}`;
      const newCommit: NodeData = {
        id: newCommitId,
        data: {
          label: commitToRebase.data!.label,
          branch: headBranch,
          type: 'rebase'
        }
      };
      contentHistory[newCommitId] = getCommitContent(commitToRebase.id as string);
      graphData.nodes.push(newCommit);
      graphData.edges.push({
        source: newHead,
        target: newCommitId,
        data: { branch: headBranch }
      });
      newHead = newCommitId;
    });

    branches[headBranch] = newHead;
    checkout(newHead);

    return `Successfully rebased '${headBranch}' onto '${baseBranch}'.`
  }

  return {
    // State
    graphData,
    branches,
    currentBranch,
    currentContent,
    head,
    isDetachedHead,
    statusMessage,
    stashStack,

    // Actions
    commit,
    createBranch,
    switchBranch,
    merge,
    checkout,
    reset,
    stash,
    stashPop,
    cherryPick,
    rebase
  }
}

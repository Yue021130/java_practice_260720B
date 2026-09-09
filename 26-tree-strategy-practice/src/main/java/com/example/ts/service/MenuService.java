package com.example.ts.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import com.example.ts.common.BusinessException;
import com.example.ts.dto.MenuAddDTO;
import com.example.ts.dto.MenuTreeVO;
import com.example.ts.entity.Menu;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 菜单服务
 *
 * <p>使用 Hutool TreeUtil 把扁平菜单列表构建成树形结构。
 * 真实业务中这里会查询数据库，本例用内存列表模拟。</p>
 */
@Service
public class MenuService {

    /** 模拟数据库主键自增 */
    private final AtomicLong idGenerator = new AtomicLong(1);

    /** 内存菜单表 */
    private final List<Menu> menuStore = new ArrayList<>();

    /**
     * 初始化演示数据
     */
    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        // 顶级菜单
        addInternal(Menu.builder().id(idGenerator.getAndIncrement()).parentId(0L).name("系统管理").path("/system").sort(1).icon("Setting").createTime(now).build());
        addInternal(Menu.builder().id(idGenerator.getAndIncrement()).parentId(0L).name("订单中心").path("/order").sort(2).icon("ShoppingCart").createTime(now).build());
        addInternal(Menu.builder().id(idGenerator.getAndIncrement()).parentId(0L).name("营销工具").path("/marketing").sort(3).icon("Present").createTime(now).build());

        // 系统管理子菜单
        addInternal(Menu.builder().id(idGenerator.getAndIncrement()).parentId(1L).name("用户管理").path("/system/user").sort(1).icon("User").createTime(now).build());
        addInternal(Menu.builder().id(idGenerator.getAndIncrement()).parentId(1L).name("角色管理").path("/system/role").sort(2).icon("UserFilled").createTime(now).build());
        addInternal(Menu.builder().id(idGenerator.getAndIncrement()).parentId(1L).name("菜单管理").path("/system/menu").sort(3).icon("Menu").createTime(now).build());

        // 用户管理子菜单（三级）
        addInternal(Menu.builder().id(idGenerator.getAndIncrement()).parentId(4L).name("用户详情").path("/system/user/detail").sort(1).icon("Document").createTime(now).build());

        // 订单中心子菜单
        addInternal(Menu.builder().id(idGenerator.getAndIncrement()).parentId(2L).name("订单列表").path("/order/list").sort(1).icon("List").createTime(now).build());
        addInternal(Menu.builder().id(idGenerator.getAndIncrement()).parentId(2L).name("退款管理").path("/order/refund").sort(2).icon("Money").createTime(now).build());

        // 营销工具子菜单
        addInternal(Menu.builder().id(idGenerator.getAndIncrement()).parentId(3L).name("优惠券").path("/marketing/coupon").sort(1).icon("Ticket").createTime(now).build());
        addInternal(Menu.builder().id(idGenerator.getAndIncrement()).parentId(3L).name("满减活动").path("/marketing/activity").sort(2).icon("TrendCharts").createTime(now).build());
    }

    private void addInternal(Menu menu) {
        menuStore.add(menu);
    }

    /**
     * 新增菜单
     */
    public Menu add(MenuAddDTO dto) {
        if (!dto.getParentId().equals(0L) && menuStore.stream().noneMatch(m -> m.getId().equals(dto.getParentId()))) {
            throw new BusinessException("父菜单不存在");
        }
        Menu menu = Menu.builder()
                .id(idGenerator.getAndIncrement())
                .parentId(dto.getParentId())
                .name(dto.getName())
                .path(dto.getPath())
                .sort(dto.getSort())
                .icon(dto.getIcon())
                .createTime(LocalDateTime.now())
                .build();
        menuStore.add(menu);
        return menu;
    }

    /**
     * 获取树形菜单
     *
     * <p>八股：Hutool TreeUtil.build 的核心配置
     * 1. idKey：节点唯一标识字段；
     * 2. parentIdKey：父节点标识字段；
     * 3. weightKey：排序权重字段；
     * 4. nameKey / extra：附加字段可通过 TreeNodeConfig 自定义。
     * </p>
     */
    public List<MenuTreeVO> tree() {
        if (CollUtil.isEmpty(menuStore)) {
            return new ArrayList<>();
        }

        TreeNodeConfig config = new TreeNodeConfig();
        // 默认就是 id / parentId / weight / name，这里显式写出便于阅读
        config.setIdKey("id");
        config.setParentIdKey("parentId");
        config.setWeightKey("sort");
        config.setNameKey("name");
        // 扩展字段默认会放进 extra，下面通过 Tree.get("path") 等获取
        config.setDeep(10);

        List<Tree<Long>> trees = TreeUtil.build(menuStore, 0L, config,
                (menu, tree) -> {
                    tree.setId(menu.getId());
                    tree.setParentId(menu.getParentId());
                    tree.setWeight(menu.getSort());
                    tree.setName(menu.getName());
                    tree.putExtra("path", menu.getPath());
                    tree.putExtra("icon", menu.getIcon());
                    tree.putExtra("sort", menu.getSort());
                });

        return convertToVO(trees);
    }

    private List<MenuTreeVO> convertToVO(List<Tree<Long>> trees) {
        if (CollUtil.isEmpty(trees)) {
            return new ArrayList<>();
        }
        return trees.stream()
                .sorted((a, b) -> {
                    int aw = a.getWeight() == null ? 0 : ((Number) a.getWeight()).intValue();
                    int bw = b.getWeight() == null ? 0 : ((Number) b.getWeight()).intValue();
                    return Integer.compare(aw, bw);
                })
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private MenuTreeVO toVO(Tree<Long> tree) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(tree.getId());
        vo.setParentId(tree.getParentId());
        vo.setName(tree.getName() == null ? null : tree.getName().toString());
        vo.setPath((String) tree.get("path"));
        vo.setIcon((String) tree.get("icon"));
        vo.setSort((Integer) tree.get("sort"));
        vo.setChildren(convertToVO(tree.getChildren()));
        return vo;
    }

    /**
     * 获取扁平菜单列表
     */
    public List<Menu> list() {
        return menuStore.stream()
                .sorted(Comparator.comparingInt(Menu::getSort))
                .collect(Collectors.toList());
    }

    /**
     * 删除菜单（同时删除其子菜单）
     */
    public void delete(Long id) {
        Menu menu = menuStore.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException("菜单不存在"));
        // 递归收集要删除的 ID
        List<Long> idsToDelete = collectIds(menu);
        menuStore.removeIf(m -> idsToDelete.contains(m.getId()));
    }

    private List<Long> collectIds(Menu menu) {
        List<Long> ids = new ArrayList<>();
        ids.add(menu.getId());
        List<Menu> children = menuStore.stream()
                .filter(m -> m.getParentId().equals(menu.getId()))
                .collect(Collectors.toList());
        for (Menu child : children) {
            ids.addAll(collectIds(child));
        }
        return ids;
    }
}

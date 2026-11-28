#!/usr/bin/env bash
# 生产 MySQL 备份脚本示例（第38章灾备节配套）
# 全量每天 + 保留 30 天；增量（binlog）按同样思路每小时归档
# 用法：DB_PASSWORD=xxx ./backup-mysql.sh   （建议 crontab 每天 02:00 执行）
set -euo pipefail

BACKUP_DIR=${BACKUP_DIR:-/data/backup/mysql}
DB=${DB:-prod_db}
KEEP_DAYS=${KEEP_DAYS:-30}
TS=$(date +%F-%H%M)

mkdir -p "$BACKUP_DIR"

# --single-transaction：InnoDB 热备不锁表；--routines：含存储过程/函数
mysqldump -u backup -p"$DB_PASSWORD" \
  --single-transaction --quick --routines --triggers \
  "$DB" | gzip > "$BACKUP_DIR/full-$DB-$TS.sql.gz"

# 保留策略：删除超过 KEEP_DAYS 天的全量备份
find "$BACKUP_DIR" -name "full-*.sql.gz" -mtime +"$KEEP_DAYS" -delete

# 备份结果输出（接入监控：非 0 即告警）
echo "backup done: full-$DB-$TS.sql.gz ($(du -h "$BACKUP_DIR/full-$DB-$TS.sql.gz" | cut -f1))"

# 恢复演练命令（每季度在隔离环境执行）：
#   gunzip < full-prod_db-XXXX.sql.gz | mysql -u root -p prod_db_restore
#   核对：表数量、行数校验和、抽查业务数据、记录耗时（RTO）

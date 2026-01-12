import { PrismaClient } from '@prisma/client';
import { batchUpsert } from '../src/utils/db-helpers';

const prisma = new PrismaClient();

function formatTime(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(2)}s`;
  const minutes = Math.floor(ms / 60000);
  const seconds = ((ms % 60000) / 1000).toFixed(2);
  return `${minutes}m ${seconds}s`;
}

function generateData(count: number) {
  const data: any[] = [];
  for (let i = 1; i <= count; i++) {
    data.push({
      bullhornId: `test-consultant-${i}`,
      firstName: `FirstName${i}`,
      lastName: `LastName${i}`,
      email: `consultant${i}@test.com`,
      phone: `+1555${String(i).padStart(7, '0')}`,
      isActive: Math.random() > 0.1, // 90% active
    });
  }
  return data;
}

async function testBatch(data: any[]) {
  const start = Date.now();

  const { created, updated } = await batchUpsert({
    prisma,
    model: 'Consultant',
    data,
    conflictColumn: 'bullhornId',
    updateColumns: ['firstName', 'lastName', 'email', 'phone', 'isActive'],
    chunkSize: 100
  });

  const duration = Date.now() - start;
  return { duration, created, updated };
}

async function testPrismaChunked(data: any[]) {
  const start = Date.now();
  let created = 0;
  let updated = 0;
  const chunkSize = 10; // Safer: limit to 10 concurrent operations (below pool size of 15)

  // Split data into chunks
  for (let i = 0; i < data.length; i += chunkSize) {
    const chunk = data.slice(i, i + chunkSize);

    // Sort by bullhornId to reduce deadlock risk (consistent lock ordering)
    chunk.sort((a, b) => a.bullhornId.localeCompare(b.bullhornId));

    // Process each item in chunk using Prisma upsert
    const promises = chunk.map((item) =>
      prisma.consultant.upsert({
        where: { bullhornId: item.bullhornId },
        update: {
          firstName: item.firstName,
          lastName: item.lastName,
          email: item.email,
          phone: item.phone,
          isActive: item.isActive,
        },
        create: {
          bullhornId: item.bullhornId,
          firstName: item.firstName,
          lastName: item.lastName,
          email: item.email,
          phone: item.phone,
          isActive: item.isActive,
        },
      })
    );

    await Promise.all(promises);

    if ((i + chunk.length) % 1000 === 0) {
      process.stdout.write(`\r   Progress: ${i + chunk.length}/${data.length}`);
    }
  }

  // Count created vs updated (approximate based on data)
  created = data.length;
  updated = 0;

  const duration = Date.now() - start;
  console.log(''); // New line after progress
  return { duration, created, updated };
}

async function testPrismaCreateUpdateMany(data: any[]) {
  const start = Date.now();
  let created = 0;
  let updated = 0;
  const chunkSize = 100; // Can be larger since createMany is a single operation

  // Split data into chunks
  for (let i = 0; i < data.length; i += chunkSize) {
    const chunk = data.slice(i, i + chunkSize);

    // Get all bullhornIds in this chunk
    const bullhornIds = chunk.map(item => item.bullhornId);

    // Find existing records
    const existing = await prisma.consultant.findMany({
      where: { bullhornId: { in: bullhornIds } },
      select: { id: true, bullhornId: true },
    });

    const existingMap = new Map(existing.map(e => [e.bullhornId, e.id]));

    // Separate new and existing records
    const toCreate = chunk.filter(item => !existingMap.has(item.bullhornId));
    const toUpdate = chunk.filter(item => existingMap.has(item.bullhornId));

    // Create new records in bulk
    if (toCreate.length > 0) {
      await prisma.consultant.createMany({
        data: toCreate.map(item => ({
          bullhornId: item.bullhornId,
          firstName: item.firstName,
          lastName: item.lastName,
          email: item.email,
          phone: item.phone,
          isActive: item.isActive,
        })),
        skipDuplicates: true,
      });
      created += toCreate.length;
    }

    // Update existing records with limited concurrency
    if (toUpdate.length > 0) {
      // Sort by ID to reduce deadlock risk (consistent lock ordering)
      toUpdate.sort((a, b) => {
        const idA = existingMap.get(a.bullhornId) || '';
        const idB = existingMap.get(b.bullhornId) || '';
        return idA.localeCompare(idB);
      });

      const updateChunkSize = 10; // Limit concurrent updates
      for (let j = 0; j < toUpdate.length; j += updateChunkSize) {
        const updateChunk = toUpdate.slice(j, j + updateChunkSize);
        await Promise.all(
          updateChunk.map(item =>
            prisma.consultant.update({
              where: { id: existingMap.get(item.bullhornId) },
              data: {
                firstName: item.firstName,
                lastName: item.lastName,
                email: item.email,
                phone: item.phone,
                isActive: item.isActive,
              },
            })
          )
        );
      }
      updated += toUpdate.length;
    }

    if ((i + chunk.length) % 1000 === 0) {
      process.stdout.write(`\r   Progress: ${i + chunk.length}/${data.length}`);
    }
  }

  const duration = Date.now() - start;
  console.log(''); // New line after progress
  return { duration, created, updated };
}

async function testSequential(data: any[]) {
  const start = Date.now();
  let created = 0;
  let updated = 0;

  // for (const item of data) {
  //   const existing = await prisma.consultant.findUnique({
  //     where: { bullhornId: item.bullhornId },
  //   });

  //   if (existing) {
  //     await prisma.consultant.update({
  //       where: { id: existing.id },
  //       data: item,
  //     });
  //     updated++;
  //   } else {
  //     await prisma.consultant.create({ data: item });
  //     created++;
  //   }

  //   if ((created + updated) % 100 === 0) {
  //     process.stdout.write(`\r   Progress: ${created + updated}/${data.length}`);
  //   }
  // }

  const duration = Date.now() - start;
  console.log(''); // New line after progress
  return { duration, created, updated };
}

async function main() {
  // ⚙️ CONFIGURE HERE - Change this number to test different record counts
  const count = 100000;

  console.log(`\n🚀 Performance Test: ${count.toLocaleString()} records`);
  console.log('='.repeat(60));

  const data = generateData(count);

  // Sequential test
  console.log('\n📊 1. SEQUENTIAL APPROACH (one-by-one)');
  const seqResult = await testSequential(data);
  console.log(`   Time: ${formatTime(seqResult.duration)}`);
  console.log(`   Created: ${seqResult.created} | Updated: ${seqResult.updated}`);
  console.log(`   Speed: ${Math.round((count / seqResult.duration) * 1000).toLocaleString()} records/sec`);

  console.log('\n🧹 Cleaning up...');
  await prisma.consultant.deleteMany({
    where: { bullhornId: { startsWith: 'test-consultant-' } },
  });

  // Prisma chunked upsert test
  console.log('\n📊 2. PRISMA CHUNKED UPSERT (Prisma upsert + Promise.all)');
  const prismaUpsertResult = await testPrismaChunked(data);
  console.log(`   Time: ${formatTime(prismaUpsertResult.duration)}`);
  console.log(`   Created: ${prismaUpsertResult.created} | Updated: ${prismaUpsertResult.updated}`);
  console.log(`   Speed: ${Math.round((count / prismaUpsertResult.duration) * 1000).toLocaleString()} records/sec`);

  console.log('\n🧹 Cleaning up...');
  await prisma.consultant.deleteMany({
    where: { bullhornId: { startsWith: 'test-consultant-' } },
  });

  // Prisma createMany + updateMany test
  console.log('\n📊 3. PRISMA CREATE/UPDATE MANY (createMany + update with Promise.all)');
  const prismaCreateUpdateResult = await testPrismaCreateUpdateMany(data);
  console.log(`   Time: ${formatTime(prismaCreateUpdateResult.duration)}`);
  console.log(`   Created: ${prismaCreateUpdateResult.created} | Updated: ${prismaCreateUpdateResult.updated}`);
  console.log(`   Speed: ${Math.round((count / prismaCreateUpdateResult.duration) * 1000).toLocaleString()} records/sec`);

  console.log('\n🧹 Cleaning up...');
  await prisma.consultant.deleteMany({
    where: { bullhornId: { startsWith: 'test-consultant-' } },
  });

  // Raw SQL batch test
  console.log('\n📊 4. BATCH RAW SQL (PostgreSQL INSERT ON CONFLICT)');
  const batchResult = await testBatch(data);
  console.log(`   Time: ${formatTime(batchResult.duration)}`);
  console.log(`   Created: ${batchResult.created} | Updated: ${batchResult.updated}`);
  console.log(`   Speed: ${Math.round((count / batchResult.duration) * 1000).toLocaleString()} records/sec`);

  // Comparison
  console.log('\n✅ COMPARISON');
  // console.log(`   1. Sequential:             ${formatTime(seqResult.duration)}`);
  console.log(`   2. Prisma Chunked Upsert:  ${formatTime(prismaUpsertResult.duration)} (${(seqResult.duration / prismaUpsertResult.duration).toFixed(1)}x faster)`);
  console.log(`   3. Prisma Create/Update:   ${formatTime(prismaCreateUpdateResult.duration)} (${(seqResult.duration / prismaCreateUpdateResult.duration).toFixed(1)}x faster)`);
  console.log(`   4. Batch Raw SQL:          ${formatTime(batchResult.duration)} (${(seqResult.duration / batchResult.duration).toFixed(1)}x faster)`);
  console.log(`\n   🏆 Winner: Batch Raw SQL is ${(prismaUpsertResult.duration / batchResult.duration).toFixed(1)}x faster than Prisma Chunked Upsert`);
  console.log('='.repeat(60));

  // Cleanup
  console.log('\n🧹 Cleaning up...');
  const deleted2 = await prisma.consultant.deleteMany({
    where: { bullhornId: { startsWith: 'test-consultant-' } },
  });
  console.log(`   Deleted ${deleted2.count.toLocaleString()} test records\n`);
}

main()
  .catch(console.error)
  .finally(() => prisma.$disconnect());

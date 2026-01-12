import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function cleanupTestData() {
  console.log('\n🧹 Cleaning up test data...');
  console.log('='.repeat(60));

  try {
    // Delete test consultants
    console.log('\n📋 Deleting test consultants...');
    const deletedConsultants = await prisma.consultant.deleteMany({
      where: {
        bullhornId: {
          startsWith: 'test-consultant-',
        },
      },
    });
    console.log(`   ✅ Deleted ${deletedConsultants.count.toLocaleString()} consultants`);

    // Delete test candidates
    console.log('\n📋 Deleting test candidates...');
    const deletedCandidates = await prisma.candidate.deleteMany({
      where: {
        bullhornId: {
          startsWith: 'test-candidate-',
        },
      },
    });
    console.log(`   ✅ Deleted ${deletedCandidates.count.toLocaleString()} candidates`);

    // Delete test positions
    console.log('\n📋 Deleting test positions...');
    const deletedPositions = await prisma.position.deleteMany({
      where: {
        bullhornId: {
          startsWith: 'test-position-',
        },
      },
    });
    console.log(`   ✅ Deleted ${deletedPositions.count.toLocaleString()} positions`);

    console.log('\n✅ Cleanup complete!');
    console.log('='.repeat(60));
  } catch (error) {
    console.error('\n❌ Error during cleanup:', error);
    throw error;
  }
}

cleanupTestData()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
